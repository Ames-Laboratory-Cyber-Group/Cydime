import logging
import os
import traceback

from twisted.internet import ssl, reactor
from twisted.internet.protocol import Factory, Protocol
from twisted.enterprise import adbapi
from twisted.internet import fdesc

from Common import valid_ip, convert_ip_to_long, on_static_whitelist
from Common import get_prediction
from Config import data_dir, default_action, cydime_host, cydime_port
from Config import server_key, server_cert, static_whitelist
from Config import threshold_file
import Config as Conf

class Cydime_Server(Protocol):

    threshold = None
    db_pool = None
    whitelist = None

    def connectionMade(self):
        client_info = self.transport.getHost()
        logging.debug('Connection to {0} port {1}'.format(client_info.host, 
                                                          client_info.port))
        self.get_threshold()
        self.read_whitelist()

        if Conf.cydime_db_engine == 'sqlite':
            dbmodule = 'sqlite3'
            db = Conf.cydime_db_location
        elif Conf.cydime_db_engine == 'postgresql':
            dbmodule = 'pyPgSQL.PgSQL'
            db = Conf.cydime_db_name
        elif Conf.cydime_db_engine == 'MySQL':
            dbmodule = 'MySQLdb'
            db = Conf.cydime_db_name
        else:
            raise CydimeDatabaseException('Unrecognized database type {0}'.format(Conf.dbmodule))

        self.db_pool = adbapi.ConnectionPool(dbmodule, db, check_same_thread=False)

    def get_threshold(self):
        '''
        get threshold from file
        '''
        fd = os.open(Conf.data_dir + '/' + Conf.threshold_file, os.O_RDONLY | os.O_CREAT)
        fdesc.readFromFD(fd, self.process_threshold)

    def read_whitelist(self):
        fd = os.open(Conf.static_whitelist, os.O_RDONLY | os.O_CREAT)
        fdesc.readFromFD(fd, self.process_whitelist)

    def process_whitelist(self, data):
        self.whitelist = set(data.split('\n'))

    def process_threshold(self, t):
        self.threshold = t

    def get_score(self, ip):
        if not valid_ip(ip):
            return '-1'
        
        ip = convert_ip_to_long(ip)
        return self.db_pool.runQuery('select score from cydime_scores where ip_addr == {0}'.format(ip))

    def send_score(self, score, ip):
        msg = ip + ','
        # check static_whitelist to make sure we don't block
        # a whitelisted IP we haven't seen before
        msg += self.response(score[0][0]) if score else self.check_static_whitelist(ip)
        self.transport.write(msg)

    def response(self, score):
        return '1' if score >= self.threshold else '0'

    def check_static_whitelist(self, ip):
        return '1' if ip in self.whitelist else '0'

    def dataReceived(self, data):
        '''
        Get score for IP, check against threshold, and return
        'host,prediction' pair.  Return -1 if data is an invalid
        ip address.
        '''
        ip = data.strip()
        self.get_score(ip).addCallback(self.send_score, ip=ip)

def run_server():

    try:
        factory = Factory()
        factory.protocol = Cydime_Server
        reactor.listenSSL(cydime_port, factory,
                          ssl.DefaultOpenSSLContextFactory(
                                server_key, 
                                server_cert))        
        reactor.run(installSignalHandlers=True)
    except Exception as e:
        if e.__doc__ is not None:
            err = 'Error: {0} {1}'.format(e.message, ' '.join(e.__doc__.split()))
        else :
            err = 'Error: {0}'.format(e.message)
        logging.error(err)
        logging.error("Type of Exception : {0}".format(type(e).__name__))
        logging.error(traceback.format_exc())

if __name__ == '__main__':
    run_server()
