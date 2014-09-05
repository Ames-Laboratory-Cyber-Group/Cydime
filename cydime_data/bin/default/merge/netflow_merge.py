#! /usr/bin/python

# this file holds the actual build/update logic for our default feature set

from __future__ import division
import logging, sys, datetime, time
from datetime import date, timedelta
from subprocess import Popen, PIPE

# container to hold feature entries as we're building
# note that we could use a list and dispense with this object entirely,
# but it's nice to be able to explicitly access fields without having to 
# resort to indices, and it's nice to initialize everything to proper values
class Feature_Dict_Item(object):

    ip_addr = "0.0.0.0"
    src_cc  = "--"
    dst_cc  = "--"
    tot_rec = 0
    tot_byt = 0
    tot_stm = 0
    tot_etm = 0
    tot_dip = 0
    tot_locpt = 0
    tot_rempt = 0
    ratio_pt = 0

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
            ip = line[0]

            if ip not in ip_dict:
                ip_count += 1
                ip_dict[ip] = Feature_Dict_Item()
                ip_dict[ip].ip_addr = ip

            ip_dict[ip].src_cc = line[1]
            ip_dict[ip].tot_rec += int(line[2])
            ip_dict[ip].tot_byt += int(line[3])
            ip_dict[ip].tot_stm += int(line[4])
            ip_dict[ip].tot_etm += int(line[5])
            ip_dict[ip].tot_dip += int(line[6])
            ip_dict[ip].tot_locpt += int(line[8])
            ip_dict[ip].tot_rempt += int(line[7])
            if ip_dict[ip].tot_rempt == 0:
                ip_dict[ip].ratio_pt = ip_dict[ip].tot_locpt
            else:
                ip_dict[ip].ratio_pt = ip_dict[ip].tot_locpt / ip_dict[ip].tot_rempt

    with open(outfilename, "r") as outfile:
        
        for line in outfile:
            if line.startswith('#') or len(line) < 1 or not line[0].isdigit():
                continue
            line = line.rstrip('\n')
            line = line.split(',')
            ip = line[0]

            found = True

            if ip not in ip_dict:
                ip_count += 1
                ip_dict[ip] = Feature_Dict_Item()
                ip_dict[ip].ip     = ip
                found = False

            ip_dict[ip].dst_cc  = line[1]
            ip_dict[ip].tot_rec += int(line[2])
            ip_dict[ip].tot_byt += int(line[3])
            if found:
                ip_dict[ip].tot_stm = min(ip_dict[ip].tot_stm, int(line[4]))
                ip_dict[ip].tot_etm = max(ip_dict[ip].tot_etm, int(line[5])) 
            else:
                ip_dict[ip].tot_stm = int(line[4])
                ip_dict[ip].tot_etm = int(line[5])
            ip_dict[ip].tot_dip += int(line[6])
            ip_dict[ip].tot_locpt = max(ip_dict[ip].tot_locpt, int(line[7]))
            ip_dict[ip].tot_rempt = max(ip_dict[ip].tot_rempt, int(line[8]))
            if ip_dict[ip].tot_rempt == 0:
                ip_dict[ip].ratio_pt = ip_dict[ip].tot_locpt
            else:
                ip_dict[ip].ratio_pt = ip_dict[ip].tot_locpt / ip_dict[ip].tot_rempt

    output_file = dest_dir + '/' + fname
    d = ip_dict
    with open(output_file, "w") as output:
        output.write("IP,src_cc,dst_cc,total_records,total_bytes,earliest_starttime,latest_endtime,total_peercount,total_localport,total_remoteport,ratio_local_remote_port\n")
        for ip in d:
            # calculate sum totals here so we don't have to expend computation
            # time when we pass features along to ML model later

            output.write('{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10}\n'\
                .format(ip, d[ip].src_cc, d[ip].dst_cc, d[ip].tot_rec,
                        d[ip].tot_byt, d[ip].tot_stm, d[ip].tot_etm,
                        d[ip].tot_dip, d[ip].tot_locpt, d[ip].tot_rempt, d[ip].ratio_pt))

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
