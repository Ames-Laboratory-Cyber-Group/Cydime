'''
Call the Java ML model.
'''

import logging

from subprocess import Popen, PIPE

from Config import bin_dir, config_dir, data_dir, java_path, java_mem

class CydimeModel(object):
    '''Main interface to talk to Java/Weka model.
    '''

    def __init__(self, date_path):
        '''setup path to daily build directory and Java command

        :param date_path: full path to daily build directory
        :type date_path: str
        '''
        self.date_path = date_path
        self.command_base = self.build_command_base()

    def build_command_base(self):
        '''construct base command used to call Java model

        :returns: str -- base command used to call Java model
        '''
        s = '{0} -Xmx{1}g -cp '.format(java_path, java_mem)
        s += bin_dir + '/Cydime.jar'
        s += ':' + config_dir
        s += ' -Djava.util.logging.config.file='
        s += config_dir + '/logging.properties'
        return s

    def build(self):
        '''preprocess feautures and run model
        '''
        self.preprocess()
        self.run()

    def preprocess(self):
        '''sort various feature files and process ARFF
        '''
        # sort first
        command = self.command_base + ' gov.ameslab.cydime.preprocess.CydimeSort ' 
        command += self.date_path
        self.exec_command(command)
        # process ARFF
        command = self.command_base + ' gov.ameslab.cydime.preprocess.CydimePreprocessor '
        command += self.date_path
        self.exec_command(command)

    def run(self):
        '''run model using command_base
        '''
        command = self.command_base + ' gov.ameslab.cydime.predictor.CydimePredictor '
        command += self.date_path
        self.exec_command(command)

    def exec_command(self, command):
        '''execute command

        :param command: command to execute
        :type command: str
        '''
        try:
            logging.info('trying command: {0}'.format(command))
            p = Popen(command.split(), stdout=PIPE, stderr=PIPE)
            output, error = p.communicate()
            logging.info(output)
            if error:
                logging.error(error)
        except Exception as e:
            logging.error(e)
            raise

