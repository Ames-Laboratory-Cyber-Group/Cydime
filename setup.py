#! /usr/bin/env python

from distutils.core import setup
import setuptools

setup(name='CYber Dynamic Impact Model Engine (cydime)',
      version='0.1',
      url='web_address_here',
      packages=['cydime'],
      license='LICENSE.txt',
      description='Cydime',
      long_description=open('README.md').read(),
      install_requires=[
        'psycopg2',
        'sqlalchemy',
        'MySQL-python',
        'netaddr',
        'scipy',
        'numpy',
        'argparse',
        'netaddr',
        'importlib',
      ],
      data_files = [('/etc/cydime/', ['conf/cydime.conf', 
                                      'conf/scraper.conf', 
                                      'conf/logging.properties',])],
      scripts = ['cydime/cydime', 'cydime/cydime-scraper'],
)
