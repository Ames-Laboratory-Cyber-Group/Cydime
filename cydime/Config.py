'''Check for all required values in cydime config file and do some
(very minimal) validation.

Other files import config values from Config.py as needed.
'''

import traceback

from ConfigParser import SafeConfigParser

# values that MUST be specified in config file
# format is {'name': 'section'}
REQUIRED = {
    'java_mem':             'AdminOptions',
    'java_path':            'AdminOptions',
    'exclude_addr':         'AdminOptions',
    'exclude_set_path':     'AdminOptions',
    'alerts_per_day':       'AdminOptions',
    'default_action':       'AdminOptions',
    'compress_old_data':    'AdminOptions',
    'data_dir':             'AdminOptions',
    'silk_threads':         'AdminOptions',
    'bin_dir':              'AdminOptions',
    'config_dir':           'AdminOptions',
    'days_to_analyze':      'AdminOptions',
    'hour_to_run':          'AdminOptions',
    'silk_map_dir':         'AdminOptions',
    'server_key':           'NetworkOptions',
    'server_cert':          'NetworkOptions',
    'cydime_host':             'NetworkOptions',
    'cydime_port':             'NetworkOptions',
    'cydime_db_engine':        'DatabaseOptions',
    'cydime_db_location':      'DatabaseOptions',
    'cydime_db_user':          'DatabaseOptions',
    'cydime_db_passwd':        'DatabaseOptions',
    'cydime_db_name':          'DatabaseOptions',
    'cydime_db_host':          'DatabaseOptions',
    'cydime_db_port':          'DatabaseOptions',
    'cydime_db_table':         'DatabaseOptions',
    'alert_db_host':        'DatabaseOptions',
    'alert_db_engine':      'DatabaseOptions',
    'alert_db_loc':         'DatabaseOptions',
    'alert_db_port':        'DatabaseOptions',
    'alert_db_name':        'DatabaseOptions',
    'alert_db_table':       'DatabaseOptions',
    'alert_db_user':        'DatabaseOptions',
    'alert_db_passwd':      'DatabaseOptions',
    'alert_db_addr_col':    'DatabaseOptions',
    'alert_db_date_col':    'DatabaseOptions',
    'log_dir':              'AdminOptions',
    'log_level':            'AdminOptions',
    'mail_level':           'AdminOptions',
    'mail_tls_port':        'AdminOptions',
    'mail_ssl_port':        'AdminOptions',
    'mail_smtp_port':       'AdminOptions',
    'mail_server':          'AdminOptions',
    'mail_user':            'AdminOptions',
    'mail_passwd':          'AdminOptions',
    'mail_to':              'AdminOptions',
    'active_labels':        'AdminOptions',
    'static_whitelist':     'AdminOptions',
    'alert_confidence':     'AdminOptions',
    'threshold_file':       'AdminOptions',
    'score_file':           'AdminOptions',
    'result_file':          'AdminOptions',
}

err = 'Fatal Error in config file: cydime not started\n'

config = SafeConfigParser()
try:
    config.read('/etc/cydime/cydime.conf')
except Exception:
    raise SystemExit('{0}{1}.'.format(err, traceback.print_exc()))

try:
    java_mem = config.get(
        REQUIRED['java_mem'], 'java_mem')
    java_path = config.get(
        REQUIRED['java_path'], 'java_path')
    silk_map_dir = config.get(
        REQUIRED['silk_map_dir'], 'silk_map_dir')
    alerts_per_day = config.getint(
                REQUIRED['alerts_per_day'], 'alerts_per_day')
    default_action = config.get(
                REQUIRED['default_action'], 'default_action')
    compress_old_data = config.getboolean(
                REQUIRED['compress_old_data'], 'compress_old_data')
    data_dir = config.get(
                REQUIRED['data_dir'], 'data_dir')
    silk_threads = config.getint(
                REQUIRED['silk_threads'], 'silk_threads')
    bin_dir = config.get(
                REQUIRED['bin_dir'], 'bin_dir')
    config_dir = config.get(
                REQUIRED['config_dir'], 'config_dir')
    days_to_analyze = config.getint(
                REQUIRED['days_to_analyze'], 'days_to_analyze')
    hour_to_run = config.getint(
                REQUIRED['hour_to_run'], 'hour_to_run')
    server_key = config.get(
                REQUIRED['server_key'], 'server_key')
    server_cert = config.get(
                REQUIRED['server_cert'], 'server_cert')
    cydime_host = config.get(
                REQUIRED['cydime_host'], 'cydime_host')
    cydime_port = config.getint(
                REQUIRED['cydime_port'], 'cydime_port')
    cydime_db_engine = config.get(
                REQUIRED['cydime_db_engine'], 'cydime_db_engine')
    cydime_db_location = config.get(
                REQUIRED['cydime_db_location'], 'cydime_db_location')
    cydime_db_user = config.get(
                REQUIRED['cydime_db_user'], 'cydime_db_user')
    cydime_db_passwd = config.get(
                REQUIRED['cydime_db_passwd'], 'cydime_db_passwd')
    cydime_db_name = config.get(
                REQUIRED['cydime_db_name'], 'cydime_db_name')
    cydime_db_host = config.get(
                REQUIRED['cydime_db_host'], 'cydime_db_host')
    cydime_db_port = config.getint(
                REQUIRED['cydime_db_port'], 'cydime_db_port')
    cydime_db_table = config.get(
                REQUIRED['cydime_db_table'], 'cydime_db_table')
    alert_db_host = config.get(
                REQUIRED['alert_db_host'], 'alert_db_host')
    alert_db_engine = config.get(
                REQUIRED['alert_db_engine'], 'alert_db_engine')
    alert_db_loc = config.get(
                REQUIRED['alert_db_loc'], 'alert_db_loc')
    alert_db_port = config.getint(
                REQUIRED['alert_db_port'], 'alert_db_port')
    alert_db_name = config.get(
                REQUIRED['alert_db_name'], 'alert_db_name')
    alert_db_table = config.get(
                REQUIRED['alert_db_table'], 'alert_db_table')
    alert_db_user = config.get(
                REQUIRED['alert_db_user'], 'alert_db_user')
    alert_db_passwd = config.get(
                REQUIRED['alert_db_passwd'], 'alert_db_passwd')
    alert_db_addr_col = config.get(
                REQUIRED['alert_db_addr_col'], 'alert_db_addr_col')
    alert_db_date_col = config.get(
                REQUIRED['alert_db_date_col'], 'alert_db_date_col')
    log_dir = config.get(
                REQUIRED['log_dir'], 'log_dir')
    log_level = config.get(
                REQUIRED['log_level'], 'log_level')
    mail_level = config.get(
                REQUIRED['mail_level'], 'mail_level')
    mail_tls_port = config.getint(
                REQUIRED['mail_tls_port'], 'mail_tls_port')
    mail_ssl_port = config.getint(
                REQUIRED['mail_ssl_port'], 'mail_ssl_port')
    mail_smtp_port = config.getint(
                REQUIRED['mail_smtp_port'], 'mail_smtp_port')
    mail_server = config.get(
                REQUIRED['mail_server'], 'mail_server')
    mail_user = config.get(
                REQUIRED['mail_user'], 'mail_user')
    mail_passwd = config.get(
                REQUIRED['mail_passwd'], 'mail_passwd')
    mail_to = config.get(
                REQUIRED['mail_to'], 'mail_to')
    active_labels = config.get(
                REQUIRED['active_labels'], 'active_labels')
    static_whitelist = config.get(
                REQUIRED['static_whitelist'], 'static_whitelist')
    alert_confidence = config.getfloat(
                REQUIRED['alert_confidence'], 'alert_confidence')
    threshold_file = config.get(
                REQUIRED['threshold_file'], 'threshold_file')
    score_file = config.get(
                REQUIRED['score_file'], 'score_file')
    result_file = config.get(
                REQUIRED['result_file'], 'result_file')
    exclude_addr = config.getboolean(
                REQUIRED['exclude_addr'], 'exclude_addr')
    exclude_set_path = config.get(
                REQUIRED['exclude_set_path'], 'exclude_set_path')


except Exception:
    exc_lines = traceback.format_exc().splitlines()
    raise SystemExit('{0}{1}'.format(err, exc_lines[-1]))

if __name__ == '__main__':
    print score_file
