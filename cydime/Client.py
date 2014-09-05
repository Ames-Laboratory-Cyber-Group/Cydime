import sys, argparse, os

from twisted.internet import ssl, reactor
from twisted.internet.protocol import ClientFactory, Protocol, ClientCreator
from twisted.internet.endpoints import SSL4ClientEndpoint

import client_config

class CydimeClient(Protocol):

    ip_list = []
    outfile = None

    def connectionMade(self):
        for i in xrange(len(self.ip_list)):
            self.transport.write(self.ip_list[i])

    def dataReceived(self, data):
        if self.outfile is not None:
            with open(self.outfile, "a") as f:
                f.write(data + '\n')
        else:
            print data
        self.transport.loseConnection()

class CydimeClientFactory(ClientFactory):

    protocol = CydimeClient

    def clientConnectionFailed(self, connector, reason):
        print "Error: {0}".format(reason.getErrorMessage())
        reactor.stop()

    def clientConnectionLost(self, connector, reason):
        print "{0}".format(reason.getErrorMessage())
        reactor.stop()

    def setArgs(self, ip_list):
        self.protocol.ip_list = ip_list

def run_client(ip_list):
    factory = CydimeClientFactory()
    factory.setArgs(ip_list)
    reactor.connectSSL(client_config.server, client_config.port, factory, ssl.ClientContextFactory())
    reactor.run()

def usage_error():
    '''
    Print out a usage string and exit if invalid arguments given.
    '''

    print '''{0} usage: 
    -f filename   --reads file of IP addresses and returns prediction
    -i IP         --returns prediction for a single IP
    -o outputfile --filename to write predictions to

    Either -f filename or -i IP *must* be specified.  Outputfile
    is optional, and if unspecified output will be written to stdout.'''.format(sys.argv[0])
    
    sys.exit(1)

def read_input(args):
    ip_list = []
    with open(sys.argv[1], "r") if len(sys.argv) > 1 else sys.stdin as f:
        for line in f:
            if line.startswith('#') or len(line) < 1:
                continue
            ip = line.rstrip('\n')
            ip_list.append(ip)

    return ip_list

if __name__=='__main__':

    ip_list = read_input(sys.argv)
    run_client(ip_list)
