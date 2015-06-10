'''
Manage building of Netflow statistical features.
'''
import traceback
import logging
from subprocess import Popen, PIPE
import os

import Netflow as NF

from Common import create_directory
from Common import get_past_scores,compute_number_of_lines

def build_installed_features(path, in_filter, out_filter):
    '''Build feature modules user has installed.

    :param path: daily build directory
    :type path: str
    :param in_filter: folder/filename of incoming records SiLK filter
    :type in_filter: str
    :param out_filter: folder/filename of outgoing records SiLK filter
    :type out_filter: str
    '''
    build_netflow_features(path, in_filter, out_filter)

def cleanup_tmp_files(path):
    '''Remove temporary build files if they still exist.

    .. note:
        this method should go away when we transition to pure Python builds.

    :param path: daily build directory
    '''
    command = '/bin/rm {0}/*.in {1}/*.out {2}/filter/tmp_cat_*'.format(path, path, path)
    p = Popen(command, shell=True, stdout=PIPE, stderr=PIPE)
    output, error = p.communicate()
    if error:
        logging.error(error)

def display_feature_files_info(path,date_list):
    '''get the number of records in each of the features files'''
    full_path = path+"/features/ip/"
    try:
        list_of_files = os.listdir(full_path)
        if len(list_of_files) != 0 :
            for i in list_of_files:
                if i != "full_netflow.features":
                    number = compute_number_of_lines(full_path+i)
                    logging.info("Number of records in file {0} : {1}".format(i, str(number)))
                    if number <=1 :
                        logging.error("Features files have no data. Hence using the past scores")
                        get_past_scores(date_list)
                        return False
        else :
            logging.error("Feature files have not been created. Hence using the past scores")
            get_past_scores(date_list)
            return False
    except Exception as e:
        logging.error(e)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())
        logging.error("Due to the above Exception, Fetching past scores")
        get_past_scores(date_list)
        return False
    return True

# XXX Yuck!  Clean this up.
def build_netflow_features(path, in_filter, out_filter):
    '''Build Netflow features.

    Call each Netflow feature user has installed.

    :param path: daily build directory
    :type path: str
    :param in_filter: folder/filename of incoming records SiLK filter
    :type in_filter: str
    :param out_filter: folder/filename of outgoing records SiLK filter
    :type out_filter: str
    '''
    n =  NF.NetflowStatFeature(path, in_filter, out_filter) 
    n.build_features()
    logging.info('Built Netflow Stat')
    n =  NF.PairServicesFeature(path, in_filter, out_filter) 
    n.build_features()
    logging.info('Built Pair Services')
    n =  NF.ServicesFeature(path, in_filter, out_filter) 
    n.build_features()
    logging.info('Built Services')
    n =  NF.TimeseriesFeature(path, in_filter, out_filter) 
    n.build_features()
    logging.info('Built Timeseries')
    n =  NF.LabelerDataFeature(path, in_filter, out_filter)
    n.build_features()
    logging.info('Built Labeler Data')

    n = NF.InternalNetflowFeature(path, in_filter, out_filter)
    n.build_features()
    logging.info('Built Internal Netflow')
    n = NF.InternalServicesFeature(path, in_filter, out_filter)
    n.build_features()
    logging.info('Built Internal Services')
    n = NF.InternalTimeseriesFeature(path, in_filter, out_filter)
    n.build_features()
    logging.info('Built Internal Timeseries')
    n = NF.InternalLabelerDataFeature(path, in_filter, out_filter)
    n.build_features()
    logging.info('Built Internal Labeler Data')
    n =  NF.FullNetflowFeature(path, path + '/filter/tmp_cat_in.silkFilter', path + '/filter/tmp_cat_out.silkFilter') 
    n.build_features()
    logging.info('Built full external netflow features.')
    n =  NF.InternalFullNetflowFeature(path, path + '/filter/tmp_cat_in.silkFilter', path + '/filter/tmp_cat_out.silkFilter') 
    n.build_features()
    logging.info('Built full internal netflow features.')

    n =  NF.PairServicesTimeseriesFeature(path, in_filter, out_filter) 
    n.build_features()
    logging.info('Built pair_services_timeseries feature.')

    cleanup_tmp_files(path)
