'''
Netflow statistical feature generation.

Each class that inherits from ORSCFeature represents a Netflow feature.

.. note::
    Features are currently built using bash scripts found in bin/, but this
    will soon be transitioned to pure Python.
'''

import logging
import sys
import os

from subprocess import Popen, PIPE, call
from os import remove

import Config as Conf

class CydimeFeature(object):
    '''Base class for Cydime Netflow features.
    '''
    
    def __init__(self, outfile, script, dest_dir, in_filter, out_filter):
        self.dest_dir    = dest_dir
        self.in_filter   = in_filter
        self.out_filter  = out_filter
        self.output_file = outfile
        self.script_name = script

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()


class NetflowStatFeature(CydimeFeature):
    '''Statistical Netflow features: records, packets, bytes, etc.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'netflow'
    output_file = 'features/ip/netflow.features'

    def __init__(self, *args, **kwargs):
        super(NetflowStatFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)


class PairServicesFeature(CydimeFeature):
    '''Service map by protocol and traffic type - internal IPs paired
    with external IPs they talk to.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'pair_services'
    output_file = 'features/ip/pair_services.features'
    
    def __init__(self, *args, **kwargs):
        super(PairServicesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -m {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
    

class ServicesFeature(CydimeFeature):
    '''Service map by protocol and traffic type.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'services'
    output_file = 'features/ip/services.features'
    
    def __init__(self, *args, **kwargs):
        super(ServicesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -m {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
    

class TimeseriesFeature(CydimeFeature):
    '''Create timeseries over filter interval using (default) hourly bins.
    '''

    prefix          = 'default/build/'
    script_name     = prefix + 'timeseries'
    partition_size  = '3600'
    output_file     = 'features/ip/timeseries.features'
    
    def __init__(self, *args, **kwargs):
        super(TimeseriesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)
    
    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -s {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, self.partition_size)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()


class ArrivalRecordFeature(CydimeFeature):
    '''Internal and external IP mapping of packet times.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'arrival_record'
    output_file = 'features/ip/arrival_record.features'
    
    def __init__(self, *args, **kwargs):
        super(ArrivalRecordFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -m {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
    

class LabelerDataFeature(CydimeFeature):
    '''Build and process data to be used by labeler.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'labeler_data'
    output_file = 'features/ip/services_timeseries.features'
    partition_size  = '3600'
    
    def __init__(self, *args, **kwargs):
        super(LabelerDataFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -s {7} -m {8}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, self.partition_size, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()

class InternalNetflowFeature(CydimeFeature):
    '''Same as NetflowStatFeature, except from the perspective of internal IPs.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'internal_netflow'
    output_file = 'features/int/netflow.features'
    
    def __init__(self, *args, **kwargs):
        super(InternalNetflowFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)
    
class InternalServicesFeature(CydimeFeature):
    '''Services feature from the perspective of internal IPs.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'internal_services'
    output_file = 'features/int/services.features'
    
    def __init__(self, *args, **kwargs):
        super(InternalServicesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -m {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()

class InternalTimeseriesFeature(CydimeFeature):
    '''Timeseries feature from perspective of internal IPs.
    '''
    
    prefix          = 'default/build/'
    script_name     = prefix + 'internal_timeseries'
    partition_size  = '3600'
    output_file     = 'features/int/timeseries.features'
    
    def __init__(self, *args, **kwargs):
        super(InternalTimeseriesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)
    
    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -s {7}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, self.partition_size)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()

class InternalLabelerDataFeature(CydimeFeature):
    '''Labeler data from perspective of internal IPs.
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'internal_labeler_data'
    output_file = 'features/int/services_timeseries.features'
    partition_size  = '3600'
    
    def __init__(self, *args, **kwargs):
        super(InternalLabelerDataFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -s {7} -m {8}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, self.partition_size, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()


class FullNetflowFeature(CydimeFeature):
    '''Statistical Netflow features: records, packets, bytes, etc.
    over entire days_to_analyze (defined in cydime.conf).
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'netflow'
    output_file = 'features/ip/full_netflow.features'

    def __init__(self, *args, **kwargs):
        super(FullNetflowFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)


class InternalFullNetflowFeature(CydimeFeature):
    '''Same as NetflowStatFeature, except from the perspective of internal IPs
    over entire days_to_analyze (defined in cydime.conf).
    '''

    prefix      = 'default/build/'
    script_name = prefix + 'internal_netflow'
    output_file = 'features/int/full_netflow.features'
    
    def __init__(self, *args, **kwargs):
        super(InternalFullNetflowFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)

class PairServicesTimeseriesFeature(CydimeFeature):

    prefix      = 'default/build/'
    script_name = prefix + 'pair_services_timeseries'
    output_file = 'features/ip/pair_services_timeseries.features'
    partition_size  = '14400'
    
    def __init__(self, *args, **kwargs):
        super(PairServicesTimeseriesFeature, self).__init__(self.output_file, 
                                                 self.script_name, 
                                                 *args, **kwargs)
    
    def build_features(self):
        command = '{0}/{1} -i {2} -o {3} -d {4} -b {5} -n {6} -s {7} -m {8}'\
                  .format(Conf.bin_dir, self.script_name, self.in_filter,
                          self.out_filter, self.dest_dir, Conf.bin_dir,
                          self.output_file, self.partition_size, Conf.silk_map_dir)  
        p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
