#! /usr/bin/python

# this file holds the actual build/update logic for our default feature set

import logging, sys, datetime, time
from datetime import date, timedelta
from subprocess import Popen, PIPE

# container to hold feature entries as we're building
# note that we could use a list and dispense with this object entirely,
# but it's nice to be able to explicitly access fields without having to 
# resort to indices, and it's nice to initialize everything to proper values
class Feature_Dict_Item(object):

    tot_rec = 0
    tot_pak = 0
    tot_byt = 0

# here we call shell scripts with args from the config file to build
# a dict of features (held in memory) to be returned to the caller
# according to my current understanding of rwuniq, in order to get the
# features we want (and properly organized), we need to call it twice:
# once for incoming and once for outgoing traffic. can this be optimized?
def merge_features(infilename, outfilename, dest_dir, fname):
    '''
    Build features for incoming and outgoing traffic, and then merge the 
    results, returning a dictionary.  Currently, we call shell scripts
    found in bin to handle the executing the SiLK commands.
    '''

    ip_dict = {}

    ip_count = 0

    with open(infilename, "r") as infile:
        # note that we're not really gaining anything efficieny-wise here by using 
        # a nested dict/custom class over a plain list; this is just so we can have 
        # explicit access to individual feature values
        for line in infile:
            if line.startswith('#') or len(line) < 1 or not line[0].isdigit():
                continue
            line = line.rstrip('\n')
            line = line.split(',')
            key = line[0] + ',' +  line[1] + ',' + line[2] + ',' + line[3] + ',' + line[4]

            if key not in ip_dict:
                ip_count += 1
                ip_dict[key] = Feature_Dict_Item()

            ip_dict[key].tot_rec += int(line[5])
            ip_dict[key].tot_pak += int(line[6])
            ip_dict[key].tot_byt += int(line[7])

    with open(outfilename, "r") as outfile:
        for line in outfile:
            if line.startswith('#') or len(line) < 1 or not line[0].isdigit():
                continue
            line = line.rstrip('\n')
            line = line.split(',')
            key = line[0] + ',' +  line[1] + ',' + line[3] + ',' + line[2] + ',' + line[4]

            if key not in ip_dict:
                ip_count += 1
                ip_dict[key] = Feature_Dict_Item()


            ip_dict[key].tot_rec += int(line[5])
            ip_dict[key].tot_pak += int(line[6])
            ip_dict[key].tot_byt += int(line[7])

    output_file = dest_dir + '/' + fname
    d = ip_dict
    with open(output_file, "w") as output:
        output.write("intIP,extIP,INproto/service,OUTproto/service,time,Records,Packets,Bytes\n")
        for ip in d:
            # calculate sum totals here so we don't have to expend computation
            # time when we pass features along to ML model later
            output.write("{0},{1},{2},{3}\n".format(ip, d[ip].tot_rec, d[ip].tot_pak, d[ip].tot_byt))
    return 

if __name__=='__main__':
    if len(sys.argv) != 5:
        sys.exit()
    # incoming features
    infile = sys.argv[1]
    # outgoing features
    outfile = sys.argv[2]
    # destination directory
    dest_dir = sys.argv[3]
    # outfile name
    fname = sys.argv[4]
    merge_features(infile, outfile, dest_dir, fname)
