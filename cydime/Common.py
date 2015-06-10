'''Common utilities used across cydime.'''

import errno
import logging
import os
import socket
import shutil
import smtplib
import struct
import traceback
import tarfile

from datetime import timedelta
from email.mime.text import MIMEText
from os import makedirs
from string import whitespace

from sqlalchemy.sql import select
from netaddr import IPNetwork, IPAddress
from subprocess import Popen, PIPE

import Config as Conf

def create_directory(full_path):
    '''Create directory 'full_path' 

    :param full_path: full path to directory to be created
    :type full_path: str
    :raises: OSError
    '''
    try:
        makedirs(full_path)
    except OSError as e:
        logging.error(e)
        logging.error("Type of Exception : OSError")
        logging.error(traceback.format_exc())
        if e.errno != errno.EEXIST:
            logging.error('Could not create directory: {0}'\
                            .format(full_path))
            raise
    except Exception as e :
        logging.error(e)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())

def convert_ip_to_long(ip):
    '''Convert IP address from 'xxx.xxx.xxx.xxx' format to long int.

    :param ip: the ip address to convert to long int representation
    :type ip: str
    :returns: int -- the converted ip address
    '''
    packedIP = socket.inet_aton(ip)
    return struct.unpack("!L", packedIP)[0]

def convert_long_to_ip(ip):
    '''Convert IP address from long int representation to 
    string representation, 'xxx.xxx.xxx.xxx'

    :param ip: long int to convert to string ip
    :type ip: int
    :returns: str -- the converted ip address
    '''
    return socket.inet_ntoa(struct.pack("!L", ip))

def valid_ip(ip):
    '''Return true iff ip is a correctly formatted IP address in string format

    :param ip: ip address to validate
    :type ip: str
    :returns: bool - true iff ip is a properly formatted ip address string
    '''
    return check_ipv6_addr(ip) if ':' in ip else check_ipv4_addr(ip)

def check_ipv6_addr(ip):
    '''Return true iff ip is a valid IPv6 address.

    .. note::
        IPv6 is currently not supported and we always return False.

    :param ip: ipv6 address to validate
    :type ip: str
    :returns: bool -- always returns false, ipv6 not currently supported
    '''
    return False

def check_ipv4_addr(ip):
    '''Return true iff ip is a valid IPv4 address.

    :param ip: ip address to validate
    :type ip: str
    :returns: bool -- true iff ip is a valid ipv4 address
    '''
    try:
        ip = ip.rstrip('\n').split('.')
    except AttributeError as e:
        logging.error(e)
        logging.error("Type of Exception : AttributeError")
        logging.error(traceback.format_exc())
        return False
    except Exception as e :
        logging.error(e)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())
    try:
        for x in xrange(len(ip)):
            for c in ip[x]:
                if c in whitespace:
                    raise ValueError
        return len(ip) == 4 and all(int(octet) < 256 for octet in ip)
    except ValueError as e:
        logging.error(e)
        logging.error("Type of Exception : ValueError")
        logging.error(traceback.format_exc())
        return False
    except Exception as e :
        logging.error(e)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())

def on_static_whitelist(ip):
    '''Return true iff ip is in the static_whitelist file.

    .. note::
        static_whitelist defined in cydime.conf

    :param ip: ip address to lookup in static whitelist
    :type ip: str
    :returns: bool -- true iff ip is found on static whitelist
    '''

    ip = ip.strip()
    ip = IPAddress(ip)

    with open(Conf.static_whitelist, 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('#'):
                continue
            if ip in IPNetwork(line):
                return True
    return False

def get_score(ip):
    '''Return score for ip.

    If ip is not found in database, return 0 if default_action == block
    and 1 if default_action == allow.

    .. note::
        default_action defined in cydime.conf

    :param ip: ip address to retrive score for
    :type ip: str
    :returns: float -- current score for ip
    '''
    from Database import get_cydime_db_engine

    engine, table = get_cydime_db_engine()  
    conn = engine.connect()

    ip = ip.strip()
    conv_ip = convert_ip_to_long(ip)

    db_string = select([table.c.score]).where\
                (table.c.ip_addr == conv_ip)
    result = conn.execute(db_string)

    res = result.fetchone()
    result.close()
    conn.close()
    if res is None:
        res = 0.0 if Conf.default_action == 'allow' else 1.0

    return res

def get_prediction(score):
    '''Return a prediction for score based on current daily threshold.

    1 if score >= threshold, 0 otherwise.

    .. note::
        new threshold is calculated each day based on alert values

    :param score: score to check against threshold
    :type score: float
    :returns: int -- 1 iff score >= current threshold, else 0
    '''
    return 1 if score >= get_threshold() else 0

def get_threshold():
    '''Return daily threshold.

    :returns: float -- current threshold
    '''
    # XXX currently just reading a text file, but this
    # could/should probably be handled better
    threshold = 0.0
    with open(Conf.data_dir + '/' + Conf.threshold_file, 'r') as f:
        threshold = float(f.readline().strip('\n'))
    return threshold

def get_date_list(current_date):
    '''Return a list of dates (strings) from current_date to a day in the past
    according to days_to_analyze.  
    
    Ex: return a list of dates from (current_date - days_to_analyze) -> current_date.

    :param current_date: today's date
    :type current_date: datetime.date
    :returns: list -- list of dates from (current_date-days_to_analyze) -> current_date
    '''
    return [str(current_date - timedelta(i)) for i in xrange(Conf.days_to_analyze)]

def get_label_dict():
    '''Return a dict of {ip: label} pairs from active labels, where label is 
    0 or 1.

    :returns: dict -- {ip: label} dict for all current ips 
    '''
    d =  {}
    with open(Conf.active_labels, 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            if not line[0].isdigit():
                continue
            line = line.split(',')
            if len(line) < 2:
                continue
            ip = line[1]
            label = float(line[0])
            ip = convert_ip_to_long(ip)
            d[ip] = label

    return d

def get_add_list(add_list, label_dict, path):
    '''Return a list of {ip_addr, score} items to insert into cydime database.

    add_list is a list to append scores to, label_dict contains, as keys,
    IPs that should be ignored here (i.e. they have an active label and
    already exist in add_list)

    :param add_list: list of IPs/scores we should add to database 
    :type add_list: list
    :param label_dict: IPs we should ignore here
    :type label_dict: dict
    :param path: path to daily directory storing our current score file
    :type path: str
    :returns: list -- a list of IPs:scores to insert into database
    '''

    with open(path + '/' + Conf.score_file, 'r') as f:
        for line in f:
            if line.startswith('#') or len(line) < 1 or not line[0].isdigit():
                continue
            line = line.rstrip('\n')
            split = line.split(',')
            conv_ip = convert_ip_to_long(split[0])
            if conv_ip in label_dict:
                continue
            else:
                score = split[1]
            add_list.append({'ip_addr': conv_ip, 'score': score})

    return add_list

def send_email(msg, subject):
    '''Send message 'msg' with subject 'subject' to mail_to

    .. note::
        mail_to defined in cydime.conf

    :param msg: text of email to send
    :type msg: str
    :param subject: subject of email
    :type subject: str
    '''

    msg = MIMEText(msg, 'plain')
    msg['To'] = Conf.mail_to
    msg['Subject'] = subject

    mailserver = smtplib.SMTP(Conf.mail_server + ':' + str(Conf.mail_tls_port))
    mailserver.starttls()
    mailserver.login(Conf.mail_user, Conf.mail_passwd)
    mailserver.sendmail(Conf.mail_user, Conf.mail_to.split(','), msg.as_string())
    mailserver.quit()

def get_daily_report(full_path):
    '''Get daily report for administrators.

    Read auto-generated result file.

    :param full_path: full path to daily build directory
    :type full_path: str
    :returns: str -- contents of report file
    '''

    try:
        with open(full_path + '/' + Conf.result_file) as f:
            text = f.read()
    except IOError as e:
        logging.error(e)
        logging.error("Type of Exception : IOError")
        logging.error(traceback.format_exc())
        if e.errno != errno.ENOENT:
            raise
        text = "Report file not found.  Time to do some labeling?"
    except Exception as e :
        logging.error(e)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())
    return text

def compress_dir(old_date):
    '''Gzip compress 'path' directory, where path is the full pathname.

    :param old_date: date to be compressed in YYYY/MM/DD format
    :type old_date: str
    '''

    path = Conf.data_dir + '/' + old_date

    if not os.path.isdir(path):
        return

    day = old_date.split('/')[-1]
    with tarfile.open(path + '.tar.gz', 'w|gz') as f:
        f.add(path, arcname=day)
    # delete directory after compressing
    shutil.rmtree(path)

def delete_old_filters(old_date):
    '''Delete filter files in directory before we compress.
    '''
    path = Conf.data_dir + '/' + old_date
    if os.path.isfile(path + '/filter/in.silkFilter'):
        os.remove(path + '/filter/in.silkFilter')
    if os.path.isfile(path + '/filter/out.silkFilter'):
        os.remove(path + '/filter/out.silkFilter')

def compute_number_of_lines(path):
    p = Popen(['/usr/bin/wc', '-l', path], stdout=PIPE,stderr=PIPE)
    result, error = p.communicate()
    if error:
        logging.error("Error in computing the lines in the file {0} : {1}".format(path,error))
        return 0
    else :
        return int(result.strip().split()[0])

def date_to_path(current_date):                                                                                   
    '''Convert current_date to file path.

    :param current_date: today's date
    :type current_date: datetime.date

    :returns: str -- full path to directory for current date
    '''
    return Conf.data_dir + '/' + str(current_date).replace('-', '/')

def get_past_scores(date_list):
    '''Fetch the latest scores from the past 2 weeks and use them for the current date's scores in case of
     build failures/errors
    '''
    try :
        for i in xrange(len(date_list)):
            full_path = '{0}/{1}/report/ip/cydime.scores'.format(Conf.data_dir, date_list[i])
            if os.path.isfile(full_path):
                number = compute_number_of_lines(full_path)
                if number <=1 :
                    continue
                else:
                    shutil.copyfile(full_path,'{0}/{1}/report/ip/cydime.scores'.format(Conf.data_dir, date_list[0]))
                    logging.info("Copied the cydime scores of date : {0}".format(date_list[i]))
                    break
    except Exception as e:
        logging.error("Error in copying past scores  : " + e.message)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())


