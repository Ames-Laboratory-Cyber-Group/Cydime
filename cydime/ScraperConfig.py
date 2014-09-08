'''Check for all required values in scraper config file and do some
(very minimal) validation.

Other files import config values from Config.py as needed.
'''

import traceback

from ConfigParser import SafeConfigParser

# values that MUST be specified in config file
# format is {'name': 'section'}
REQUIRED = {
    'scraper_db_engine':        'DatabaseOptions',
    'scraper_db_location':      'DatabaseOptions',
    'scraper_db_user':          'DatabaseOptions',
    'scraper_db_passwd':        'DatabaseOptions',
    'scraper_db_name':          'DatabaseOptions',
    'scraper_db_host':          'DatabaseOptions',
    'scraper_db_port':          'DatabaseOptions',
    'scraper_db_table':         'DatabaseOptions',
    'scraper_table':            'DatabaseOptions',
    'scraper_tmp_engine':        'DatabaseOptions',
    'scraper_tmp_location':      'DatabaseOptions',
    'scraper_tmp_user':          'DatabaseOptions',
    'scraper_tmp_passwd':        'DatabaseOptions',
    'scraper_tmp_name':          'DatabaseOptions',
    'scraper_tmp_host':          'DatabaseOptions',
    'scraper_tmp_port':          'DatabaseOptions',
    'scraper_tmp_table':         'DatabaseOptions',
    'expire_interval':          'DatabaseOptions',
    'retire_interval':          'DatabaseOptions',
    'data_dir':                 'FileOptions',
    'ip_list':                  'FileOptions',
    'threshold':                'ScraperOptions',
    'dst_dir':                  'ScraperOptions',
    'crawler.ip_dom':           'CrawlerOptions',
    'crawler.ip_whois':         'CrawlerOptions',
    'crawler.mission.merged':   'CrawlerOptions',
    'crawler.web.merged':       'CrawlerOptions',
    'crawler.web.output':       'CrawlerOptions',
    'label.ip_dom':             'ModelOptions',
    'label.ip_whois':           'ModelOptions',
    'label.dom_doc':            'ModelOptions',
    'lexical.mission.sim':      'ModelOptions',
    'crawler.dom_doc':          'CrawlerOptions',
    'crawler.mission.sim':      'CrawlerOptions',
    'java_path':                'ScraperOptions',
    'mallet_path':              'ScraperOptions',
    'crawler.mission.output':   'CrawlerOptions',
}

err = 'Fatal Error in config file: scraper not started\n'

config = SafeConfigParser()
try:
    config.read('/etc/cydime/scraper.conf')
except Exception:
    raise SystemExit('{0}{1}.'.format(err, traceback.print_exc()))

try:
    crawler_mission_output = config.get(
                REQUIRED['crawler.mission.output'], 'crawler.mission.output')
    java_path = config.get(
                REQUIRED['java_path'], 'java_path')
    mallet_path = config.get(
                REQUIRED['mallet_path'], 'mallet_path')
    scraper_host = config.get(
                REQUIRED['scraper_db_host'], 'scraper_db_host')
    scraper_port = config.getint(
                REQUIRED['scraper_db_port'], 'scraper_db_port')
    scraper_db_engine = config.get(
                REQUIRED['scraper_db_engine'], 'scraper_db_engine')
    scraper_db_location = config.get(
                REQUIRED['scraper_db_location'], 'scraper_db_location')
    scraper_db_user = config.get(
                REQUIRED['scraper_db_user'], 'scraper_db_user')
    scraper_db_passwd = config.get(
                REQUIRED['scraper_db_passwd'], 'scraper_db_passwd')
    scraper_db_name = config.get(
                REQUIRED['scraper_db_name'], 'scraper_db_name')
    scraper_db_host = config.get(
                REQUIRED['scraper_db_host'], 'scraper_db_host')
    scraper_db_port = config.getint(
                REQUIRED['scraper_db_port'], 'scraper_db_port')
    scraper_db_table = config.get(
                REQUIRED['scraper_db_table'], 'scraper_db_table')
    scraper_tmp_host = config.get(
                REQUIRED['scraper_tmp_host'], 'scraper_tmp_host')
    scraper_tmp_port = config.getint(
                REQUIRED['scraper_tmp_port'], 'scraper_tmp_port')
    scraper_tmp_engine = config.get(
                REQUIRED['scraper_tmp_engine'], 'scraper_tmp_engine')
    scraper_tmp_location = config.get(
                REQUIRED['scraper_tmp_location'], 'scraper_tmp_location')
    scraper_tmp_user = config.get(
                REQUIRED['scraper_tmp_user'], 'scraper_tmp_user')
    scraper_tmp_passwd = config.get(
                REQUIRED['scraper_tmp_passwd'], 'scraper_tmp_passwd')
    scraper_tmp_name = config.get(
                REQUIRED['scraper_tmp_name'], 'scraper_tmp_name')
    scraper_tmp_host = config.get(
                REQUIRED['scraper_tmp_host'], 'scraper_tmp_host')
    scraper_tmp_port = config.getint(
                REQUIRED['scraper_tmp_port'], 'scraper_tmp_port')
    scraper_tmp_table = config.get(
                REQUIRED['scraper_tmp_table'], 'scraper_tmp_table')
    expire_interval = config.getint(
                REQUIRED['expire_interval'], 'expire_interval')
    retire_interval = config.getint(
                REQUIRED['retire_interval'], 'retire_interval')
    data_dir = config.get(
                REQUIRED['data_dir'], 'data_dir')
    ip_list = config.get(
                REQUIRED['ip_list'], 'ip_list')
    threshold = config.getint(
                REQUIRED['threshold'], 'threshold')
    dst_dir = config.get(
                REQUIRED['dst_dir'], 'dst_dir')
    crawler_ip_dom = config.get(
                REQUIRED['crawler.ip_dom'], 'crawler.ip_dom')
    crawler_ip_whois = config.get(
                REQUIRED['crawler.ip_whois'], 'crawler.ip_whois')
    crawler_mission_merged = config.get(
                REQUIRED['crawler.mission.merged'], 'crawler.mission.merged')
    crawler_web_output = config.get(
                REQUIRED['crawler.web.output'], 'crawler.web.output')
    crawler_web_merged = config.get(
                REQUIRED['crawler.web.merged'], 'crawler.web.merged')
    label_ip_dom = config.get(
                REQUIRED['label.ip_dom'], 'label.ip_dom')
    label_ip_whois = config.get(
                REQUIRED['label.ip_whois'], 'label.ip_whois')
    label_dom_doc = config.get(
                REQUIRED['label.dom_doc'], 'label.dom_doc')
    crawler_dom_doc = config.get(
                REQUIRED['crawler.dom_doc'], 'crawler.dom_doc')
    lexical_mission_sim = config.get(
                REQUIRED['lexical.mission.sim'], 'lexical.mission.sim')
    crawler_mission_sim = config.get(
                REQUIRED['crawler.mission.sim'], 'crawler.mission.sim')
except Exception:
    exc_lines = traceback.format_exc().splitlines()
    raise SystemExit('{0}{1}'.format(err, exc_lines[-1]))

if __name__ == '__main__':
    pass
