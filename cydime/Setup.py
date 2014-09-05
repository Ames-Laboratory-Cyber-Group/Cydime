'''
Setup initial environment before running a build.
'''

import logging, os, errno
import traceback
import importlib

import Config as Conf

def setup_logging():
    '''
    Initialize Cydime log directory and log file.
    '''

    # make sure we can create the log directory if it's not already there
    try:
        os.makedirs(Conf.log_dir)
    except (IOError, OSError) as e:
        if e.errno != errno.EEXIST:
            exc_lines = traceback.format_exc().splitlines()
            raise SystemExit('Failed to create log directory.\n{0}'\
                        .format(exc_lines[-1]))

    # check the log_level user has set 
    # default to INFO if a invalid level was chosen
    level = getattr(logging, Conf.log_level.upper(), None)
    level_error = False
    if not isinstance(level, int):
        level = getattr(logging, 'INFO', None)
        level_error = True

    try:
        logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            datefmt='%m/%d/%Y %I:%M:%S %p',
            filename=Conf.log_dir + '/cydime_log.txt',
            level=level,
        )
        logging.getLogger('sqlalchemy.engine').setLevel(logging.ERROR)
    except Exception:
        exc_lines = traceback.format_exc().splitlines()
        raise SystemExit('Cannot write to log file.\n{0}'\
                    .format(exc_lines[-1]))

    if level_error:
        logging.error('Invalid log level "{0}". Defaulting to "INFO"'
                    .format(Conf.log_level))
