'''
Calculate a daily threshold.

The threshold is what determines a response of 1 or 0.  A score >= the current
threshold means a 1 response, and any score < current threshold means a response
of 0.

The threshold is calculated such that we can claim with the desired level of
confidence (default 99%) that an analyst will not see more than the desired
amount of alerts in a given time period (default 24 hours).

We assume a Poisson distribution over the number of alerts we get per day, 
and estimating the lambda parameter of that distribution by looking at how 
many alerts we received per day in the past X days using a desired maximum
of N alerts per day, we then determine what threshold would produce that number
or fewer by using the Poisson probability mass function.
'''

import logging
import math
import traceback

from scipy.stats import poisson
from numpy import ndarray,array
import numpy as np

from datetime import timedelta, date
from urllib import quote

from sqlalchemy import create_engine, exc, MetaData
from sqlalchemy.sql import select

import Common as Com
import Config as Conf

from Database import get_alert_db_engine, get_cydime_db_engine

def update_threshold(full_path, current_date):
    '''Calculate threshold and write to file.

    :param full_path: full path to daily build directory
    :type full_path: str
    :param current_date: today's date
    :type current_date: datetime.date
    '''
    threshold = find_threshold(current_date)
    write_threshold(full_path + '/report', threshold)
    
def write_threshold(full_path, threshold):
    '''Write new threshold file.

    :param full_path: full path to daily build directory
    :type full_path: str
    :param threshold: new threshold
    :type threshold: float
    '''
    with open(full_path + '/' + Conf.threshold_file, 'w') as f:
        f.write('{0}\n'.format(str(threshold)))

def find_threshold(today):
    '''Find daily alert threshold.

    :param today: today's date
    :type today: datetime.date
    :returns: float -- calculated threshold
    '''

    date_list = Com.get_date_list(today)
    ip_set = get_alert_set(date_list)
    dataset = build_alert_scores(ip_set, False)

    return poisson_threshold(dataset)

def poisson_threshold(dataset):
    """
    Given a date,
    return a threshold value which will produce alerts_per_day or more instances
    in a day with probability p or less.  That is:
    p(X > alerts_per_day) <= admin_conf.alert_confidence.

    :param dataset: sorted numpy array of alert scores
    :type dataset: numpy array
    :returns: float -- calculated threshold
    """
    # First, find the target parameter
    mu = Conf.alerts_per_day
    amount = mu
    error = 1
    iters = 0

    while error > Conf.alert_confidence and iters < 100:
        # Keep trying until we get closer
        iters = iters + 1
        amount = float (amount) / 2
        prob = 1 - poisson.cdf(Conf.alerts_per_day, mu)
        error = math.fabs(Conf.alert_confidence - prob)
        if prob > Conf.alert_confidence:
            # Need to keep decreasing lambda
            mu = mu - amount
        else:
            # We overshot
            mu = mu + amount

    # Now, figure out the threshold so that the average number of investigations
    # per day is mu.  I think we can just take the score of the mu'th instance
    # for each day, and then average those. 
    numDays = 0
    scoreSum = 0
    # if we don't have enough alerts for a given day to use a score, just 
    # use zero for that day's scoreSum - i.e. we want to see all alerts
    for npArray in dataset:
        if len(npArray) > int(mu):
            scoreSum += npArray[int(mu)]
        numDays = numDays + 1

    return 0.0 if numDays == 0 else float(scoreSum) / numDays

def get_alert_set(date_list):
    '''
    Return a list of sets (one set for each day) of IPs we got alerts for.

    :param date_list: list of dates we should get alerted IPs for
    :type date_list: array (str)
    :returns: array (set) -- array of sets for each day in date list, where each set
        contains the IPs (str) that had an alert that day
    '''
    
    alist = []

    engine, table = get_alert_db_engine()
    conn = engine.connect()
    if conn:
        for i in xrange(len(date_list)):
            query = 'select {0} from {1} where \
                     {2} >= "{3}" and "{4}" > {5}'\
                .format(Conf.alert_db_addr_col, Conf.alert_db_table, 
                        Conf.alert_db_date_col, date_list[i] + ' 00:00:00', 
                        date_list[i] + ' 23:59:59', Conf.alert_db_date_col)
            res = conn.execute(query)
            ip_set = set()
            for i in res:
                ip_set.add(getattr(i, Conf.alert_db_addr_col))
            alist.append(ip_set)

        conn.close()
    else:
        logging.error('Couldn\'t establish database connection in get_threshold')

    return alist

def build_alert_scores(alist, notFound=False):
    '''
    Return sorted numpy array of alert scores. 

    :param alist: array of sets for each day in date list, where each set
        contains the IPs (str) that had an alert that day
    :type alist: array of sets containing strings
    :param notFound: whether or not we should count alerted IPs we don't have a
        score for when computing the threshold
    :type notFound: bool
    :returns: an array of sorted numpy arrays, where each numpy array contains
        a list of the scores for alerted IPs for that day
    '''

    engine, cydime_default = get_cydime_db_engine()
    conn = engine.connect()

    xlist = []

    if conn:
        for i in xrange(len(alist)):
            tmp = []
            for ip in alist[i]:
                ip = ip.rstrip('\n')

                try:
                    conv_ip = Com.convert_ip_to_long(ip)
                except socket.error as e:
                    logging.error(e)
                    logging.error("Type of Exception : socket.error")
                    logging.error(traceback.format_exc())
                    logging.error('Invalid IP {0} returned from database while building threshold.'.format(ip))
                    continue
                except Exception as ex:
                    logging.error(ex)
                    logging.error("Type of Exception : {0}".format(type(ex).__name__))
                    logging.error(traceback.format_exc())
                    continue

                db_string = select([cydime_default.c.score]).where\
                    (cydime_default.c.ip_addr==conv_ip)
                res = conn.execute(db_string)
                r = res.fetchone()
                if r is not None:
                    tmp.append(r[0])
                else:
                    if notFound is True:
                        tmp.append(0.0)

            nparray = array(tmp)
            nparray.sort()
            xlist.append(nparray[:-Conf.alerts_per_day-1:-1])
        conn.close()
    else:
        logging.error('Couldn\'t establish remote database connection to get CFM alerts.')

    return xlist
