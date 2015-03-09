#! /usr/bin/env python

from __future__ import print_function

import argparse
import ipaddress

from bisect import bisect_left


class ASNObj(object):
    
    def __init__(self, low, high, asn, string):
        self.low        = low
        self.high       = high
        self.asn        = int(asn.replace('AS', ''))
        self.string     = string
        self.ip_low     = str(ipaddress.IPv4Address(int(low)))
        self.ip_high    = str(ipaddress.IPv4Address(int(high)))

    def __str__(self):
        s =  self.ip_low + ',' + self.ip_high + ',AS'
        s += str(self.asn) + ',' + self.string
        return s

    def __eq__(self, other):
        # for bisect module purposes, we consider something "equal" to 
        # this object if it falls with range of low,high AS number
        return other >= self.low and other <= self.high

    def __lt__(self, other):
        return self.low < other

    def __gt__(self, other):
        return self.low > other


def build_asn_list(asn_fpath):
    '''Expect a MaxMind 'GeoIPASNum2.csv' formatted file.
    '''
    a = []
    with open(asn_fpath, 'r') as f:
        for line in f:
            line = line.replace('"', '').strip().split(',')
            low = int(line[0])
            high = int(line[1])
            asn_str = ' '.join(line[2:])
            asn_str = asn_str.split(' ')
            asn = asn_str[0]
            string = ' '.join(asn_str[1:])
            a.append(ASNObj(low, high, asn, string))
    
    return a


def asn_binsearch(addr, alist, lo=0, hi=None):
    '''do a binary search on IPs and return some candidates to
    check for ASN membership
    '''
    hi  = hi if hi is not None else len(alist)
    pos = bisect_left(alist, addr, lo, hi)
    pos = pos if alist[pos] == addr else pos - 1

    return pos if pos != hi and alist[pos] == addr else None


def asn_map(asn_list, ip_list, outfile):
    with open(outfile, 'w') as f:
        for ip in ip_list:
            x =  asn_binsearch(int(ip), asn_list)
            if x is None:
                f.write('{0},NA,NA\n'.format(ip))
            else:
                f.write('{0},{1},{2}\n'.format(ip, asn_list[x].asn, 
                                               asn_list[x].string))


def get_IPs_from_file(ip_fpath):

    ip_set = set()

    with open(ip_fpath, 'r') as f:
        for line in f:
            if not line[0].isdigit():
                continue
            ip = line.split(',')[0]
            ip = ipaddress.IPv4Address(unicode(ip))
            #if ip.is_private:
            #    raise ValueError('{0} is a private IP Address.'.format(ip))
            ip_set.add(ip)

    return ip_set


def build_asn_map(ip_fpath, asn_fpath, outfile_path):
    ip_set = get_IPs_from_file(ip_fpath)
    alist = build_asn_list(asn_fpath)
    asn_map(alist, ip_set, outfile_path)
