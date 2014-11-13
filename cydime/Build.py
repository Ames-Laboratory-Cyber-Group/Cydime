import logging
import sys

from datetime import date, datetime, timedelta

import Config as Conf
import Common as Com
from Errors import CydimeDatabaseException
from Database import insert_scores
from FeatureInterface import build_installed_features
from FlowGenerator import filter_records
from ModelInterface import CydimeModel
from Threshold import update_threshold

def create_daily_dirs(path):
    '''Create needed directories for each day's information.

    :param path: full path to daily build directory
    :type path: str
    '''
    Com.create_directory(path + '/filter')
    Com.create_directory(path + '/features')
    Com.create_directory(path + '/preprocess')
    Com.create_directory(path + '/report')
    Com.create_directory(path + '/model')

def calc_date(end, interval):
    '''Return (date_list, start_date, end_date, todays_date) tuple.

    :param end: ending filter date (latest day)
    :type end: str
    :param interval: interval over which we filter (in days)
    :type interval: int
    :returns: tuple(list, datetime.date) -- list of YYYY/MM/DD dates 
        to include in the filter, today's date as datetime.date object
    '''
    today = date.today()

    if isinstance(end, str):
        end = datetime.strptime(end, '%Y/%m/%d')

    date_list = [str(end-timedelta(x)).replace('-', '/').split(' ')[0] for x in xrange(0, interval)]

    return (date_list, date.today())

def build(end, interval=None, update=True, mail=False, initial=False):
    '''Run a daily build.

    :param end: ending filter date (latest day)
    :type end: datetime.date
    :param interval: interval over which we filter (in days)
    :type interval: int
    :param update: update database with built scores if true
    :type update: boolean
    :param mail: whether or not to mail the administrators
    :type mail: boolean

    build filters SiLK records, extracts features, runs the model, and handles
    inserting scores into a database and general archiving and cleanup.
    '''

    try:
        date_list, current_date = calc_date(end, interval)
        start_date = str(date_list[-1]).replace('-', '/').split(' ')[0]
        end_date = str(date_list[0]).replace('-', '/').split(' ')[0]

        full_path = Conf.data_dir + '/' + end_date

        create_daily_dirs(full_path)

        filter_records(full_path, start_date, end_date, date_list)
        logging.info('Filtered records from {0} to {1}'.format(start_date, end_date))

        # XXX Need way to abstract the 'silkFilter' name to whatever backend 
        # used this should be done in FlowGenerator.py
        build_installed_features(full_path,
                                 full_path + '/filter/in.silkFilter',
                                 full_path + '/filter/out.silkFilter')
        logging.info('Features built for {0}'.format(end_date))

        # we're done if this is an initial build
        if initial:
            return

        m = CydimeModel(end_date)
        m.build()
        logging.info('Model successfully built on {0}'.format(end_date))

        logging.info('score_file: {0}'.format(Conf.score_file))

        # only update score db and threshold if we're doing
        # an automated build or we explicitly say to
        if update:
            insert_scores(full_path)
            update_threshold(full_path, current_date)

        # only send email on automated builds
        if mail:
            Com.send_email(Com.get_daily_report(full_path), 'Cydime-Dev Update')
        Com.delete_old_filters(start_date)
        if Conf.compress_old_data:
            Com.compress_dir(start_date)
    except Exception as e:
        # no need to email if doing manual build
        if not mail:
            raise
        Com.send_email('Uh-oh, Cydime-Dev had an error: {0}'.format(e), 'Cydime-Dev ERROR')
        logging.error(e)

if __name__ == '__main__':
    import argparse
    from datetime import datetime

    parser = argparse.ArgumentParser()
    parser.add_argument('-e', '--end',
                        help='ending filter date in yyyy/mm/dd format.\n'
                        + 'defaults to today.')
    parser.add_argument('-u', '--update',
                        help='updates the current score database.\n'
                        + "anything other than 'true' here means no.")
    parser.add_argument('-i', '--interval',
                        help="total length of the filter interval in days.\n"
                        + "defaults to 14.")
    parser.add_argument('-n', '--initial',
                        help="do initial build without running model.\n")
    args = parser.parse_args()

    try:
        if args.end:
            args.end = datetime.strptime(args.end.replace('/', '-'), '%Y-%m-%d')
    except ValueError:
        raise ValueError('Incorrectly formatted end date.')

    if not args.end:
        args.end = date.today()
    if not args.interval:
        args.interval = Conf.days_to_analyze
    else:
        args.interval = int(args.interval)

    build(args.end, args.interval, args.update == 'True', True,
            args.initial != None)
