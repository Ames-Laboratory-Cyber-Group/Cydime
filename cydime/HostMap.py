import sys

from twisted.internet import defer, task, reactor
from twisted.names import client

def reverseNameFromIP(address):
    return '.'.join(reversed(address.split('.'))) + '.in-addr.arpa'


def writeResult(result, address, outFile):
    answers, authority, additional = result
    if answers:
        a = answers[0]
        outFile.write('{0},{1}\n'.format(address, a.payload.name))


def lookupFailed(reason, address, outFile):
    outFile.write('{0},NA\n'.format(address))


def reverseLookup(address, outFile):
    d = client.lookupPointer(name=reverseNameFromIP(address), timeout=(1, 3, 5, 10))
    d.addCallback(writeResult, address, outFile)
    d.addErrback(lookupFailed, address, outFile)
    return d


def die(result, outFile):
    outFile.close()
    reactor.stop()


def listFailure(reason):
    print 'list failed: {0}'.format(reason)


def partition(lst, size):
    return [lst[i::size] for i in xrange(size)]


def buildHostMap(ipFile, outFilename):

    reactor.installResolver(client.createResolver(servers=[('127.0.0.1', 53), ('8.8.8.8', 53), ('8.8.4.4', 53)]))
    CHUNK_SIZE = 20000
    
    ipList = []
    with open(ipFile, 'r') as f:
        for line in f:
            if line[0].isdigit():
                ipList.append(line.split(',')[0])

    outFile = open(outFilename, 'w')

    def ret(dummy, chunk, outFile):
        return defer.DeferredList([reverseLookup(ip, outFile) for ip in chunk])

    ipList = [ipList[i:i+CHUNK_SIZE] for i in xrange(0, len(ipList), CHUNK_SIZE-1)]

    d = defer.Deferred()
    for chunk in ipList:
        d.addCallback(ret, chunk, outFile)
    # cleanup after ourselves
    d.addCallback(die, outFile)
    d.callback(None)

    reactor.run()
