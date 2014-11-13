'''
Filter records on user defined internal to use for feature extraction.
'''

import errno
import logging
import os
import sys

from datetime import datetime, timedelta
from subprocess import Popen, PIPE

import Config as Conf

from Common import create_directory

def validate_date_list(date_list):
    '''Remove dates from date_list if the file doesn't exist.

    :param date_list: list of dates we should try to include in filter set
    :type date_list: list

    :returns: list -- list of dates to include in filter (non-existant filter dates removed)
    '''
    missing_dates = []

    for i in date_list[:]:
        in_path = '{0}/{1}/filter/in.silkFilter'.format(Conf.data_dir, i)
        out_path = '{0}/{1}/filter/out.silkFilter'.format(Conf.data_dir, i)
        if not os.path.isfile(in_path) or not os.path.isfile(out_path):
            date_list.remove(i)
            missing_dates.append(i)

    if len(missing_dates) > 0:
        logging.error('Missing SiLK filters for the following dates: {0}'\
            .format(','.join(missing_dates)))

    return date_list

def filter_records(full_path, start_date, end_date, date_list):
    '''
    Select a backend to filter records.
    
    .. note::
        Currently only support SiLK

    :param full_path: full path to daily build directory
    :type full_path: str
    :param start_date: starting date (inclusive) for filter interval
    :type start_date: str
    :param end_date: ending date (inclusive) for filter interval
    :type end_date: str
    :param date_list: list of dates to include in filter
    :type date_list: list
    '''

    yesterday = datetime.strptime(end_date, '%Y/%m/%d')
    yesterday = yesterday - timedelta(1)
    yesterday = str(yesterday).replace('-', '/').split()[0]

    silk_in_filter_single(full_path, yesterday, end_date)
    silk_out_filter_single(full_path, yesterday, end_date)

    # make sure each day's filter actually exists
    # if not, log it and remove from date_list
    date_list = validate_date_list(date_list)

    silk_in_filter_cat(full_path, date_list)
    silk_out_filter_cat(full_path, date_list)

def silk_in_filter_cat(full_path, date_list):
    '''Concatenate each day in date_list's filter for timeseries.

    :param full_path: full path to current day's directory
    :type full_path: str
    :param date_list: list of dates with filters to concatenate
    :type date_list: list
    '''
    command = 'rwcat '
    for i in xrange(len(date_list)):
        command += '{0}/{1}/filter/in.silkFilter '.format(Conf.data_dir, date_list[i])
    command += ' --output-path={0}'.format(full_path + '/filter/tmp_cat_in.silkFilter')
    p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
    output, error = p.communicate()
    if error:
        logging.error(error)


def silk_out_filter_cat(full_path, date_list):
    '''Concatenate each day in date_list's filter for timeseries.

    :param full_path: full path to current day's directory
    :type full_path: str
    :param date_list: list of dates with filters to concatenate
    :type date_list: list
    '''
    command = 'rwcat '
    for i in xrange(len(date_list)):
        command += '{0}/{1}/filter/out.silkFilter '.format(Conf.data_dir, date_list[i])
    command += ' --output-path={0}'.format(full_path + '/filter/tmp_cat_out.silkFilter')
    p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
    output, error = p.communicate()
    if error:
        logging.error(error)

def silk_out_filter_single(full_path, start_date, end_date):
    '''
    Filter outgoing records using SiLK for a single day.

    :param full_path: full path to daily build directory
    :type full_path: str
    :param start_date: starting date (inclusive) for filter interval
    :type start_date: str
    :param end_date: ending date (inclusive) for filter interval
    :type end_date: str
    '''
    

    out_dst = full_path + '/filter/' + 'out.silkFilter'
    command = 'rwfilter --start-date={0} --end-date={1} --type=all '\
              .format(start_date + ':' + str(Conf.hour_to_run), end_date + ':' + str(Conf.hour_to_run - 1))
    if Conf.exclude_addr:
        command += '--not-anyset={0} '.format(Conf.exclude_set_path)
    command += '--dtype=2 --threads={0} --pass-destination={1}'\
                         .format(Conf.silk_threads, out_dst)
    p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
    output, error = p.communicate()
    if error:
        logging.error(error)

def silk_in_filter_single(full_path, start_date, end_date):
    '''
    Filter incoming records using SiLK for a single day.

    :param full_path: full path to daily build directory
    :type full_path: str
    :param start_date: starting date (inclusive) for filter interval
    :type start_date: str
    :param end_date: ending date (inclusive) for filter interval
    :type end_date: str
    '''

    in_dst = full_path + '/filter/' + 'in.silkFilter'
    command = 'rwfilter --start-date={0} --end-date={1} --type=all '\
              .format(start_date + ':' + str(Conf.hour_to_run), end_date + ':' + str(Conf.hour_to_run))
    if Conf.exclude_addr:
        command += '--not-anyset={0} '.format(Conf.exclude_set_path)
    command += '--stype=2 --threads={0} --pass-destination={1}'\
                         .format(Conf.silk_threads, in_dst)
    
    p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
    output, error = p.communicate()
    if error:
        logging.error(error)
