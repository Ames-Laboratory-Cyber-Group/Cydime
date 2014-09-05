'''
Cydime database functions.
'''

import logging

from sqlalchemy import Table, Column, Integer, BigInteger, Sequence, Float 
from sqlalchemy import create_engine, MetaData
from sqlalchemy.exc import SQLAlchemyError
from urllib import quote

import Common as Com
import Config as Conf

from Errors import CydimeDatabaseException

def get_cydime_db_engine():
    '''Return sqlalchemy engine and table schema for cydime scores.

    :returns: tuple -- (engine, schema), sqlalchemy engine and schema for cydime
        score table
    '''

    db_string = ''
    e = Conf.cydime_db_engine.lower()
    if e == 'sqlite':
        db_string = e + ':///' + Conf.cydime_db_location
    elif e == 'postgresql' or e == 'mysql':
        port = '' if Conf.cydime_db_port == 0 else ':' + Conf.cydime_db_port
        db_string = '{0}://{1}:{2}@{3}{4}'.format(e, Conf.cydime_db_user, 
                                                  quote(Conf.cydime_db_passwd),
                                                  Conf.cydime_db_host, port)
        db_string += '/' + Conf.cydime_db_name
    else:
        raise CydimeDatabaseException('Unsupported Database Type {0}'\
                                    .format(Conf.cydime_db_engine))

    try:
        engine = create_engine(db_string, echo=False)
        metadata = MetaData()

        cydime_default = Table(Conf.cydime_db_table, metadata,
                Column('id', Integer, Sequence('id_seq'), primary_key=True),
                # BigInteger so we can support IPv6 (eventually)
                Column('ip_addr', BigInteger, Sequence('ip_addr_seq'),
                       index=True, nullable=False),
                Column('score', Float, nullable=False),
        )

        metadata.create_all(engine)
    except SQLAlchemyError as e:
        raise CydimeDatabaseException(e.message)

    return (engine, cydime_default)

def get_alert_db_engine():
    '''Return an sqlalchemy engine and table schema for alerts db.

    .. note::
        Use database reflection to figure out alert db schema (we don't
        create the alert db and assume it exists).

    :returns: tuple -- (engine, schema), sqlalchemy engine and schema for
        accessing remote alert db
    '''

    db_string = ''
    e = Conf.alert_db_engine.lower()
    if e == 'sqlite':
        db_string = e + ':///' + Conf.alert_db_loc
    elif e == 'postgresql' or e == 'mysql':
        port = '' if Conf.alert_db_port == 0 else ':' + str(Conf.alert_db_port)
        db_string = '{0}://{1}:{2}@{3}{4}'.format(e, Conf.alert_db_user, 
                                                  quote(Conf.alert_db_passwd),
                                                  Conf.alert_db_host, port)
        db_string += '/' + Conf.alert_db_name
    else:
        raise CydimeDatabaseException('Unsupported Alert Database Type {0}'\
                                    .format(Conf.alert_db_engine))

    try:
        meta = MetaData()
        engine = create_engine(db_string, echo=False)
        table = Table(Conf.alert_db_table, meta, autoload=True, autoload_with=engine)
    except SQLAlchemyError as e:
        raise CydimeDatabaseException(e.message)

    return (engine, table)

def insert_scores(path):
    '''Insert scores from cydime.scores into cydime db score table.

    :param path: full path to daily build directory
    :type path: str
    '''
    
    engine, table = get_cydime_db_engine()

    conn = engine.connect()
    add_count = 0
    add_list = []

    label_dict = Com.get_label_dict()
    add_list = [{'ip_addr': i, 'score': label_dict[i]} for i in label_dict]
    add_list = Com.get_add_list(add_list, label_dict, path)
    add_count = len(add_list)

    if add_count > 0:
        try:
            conn.execute(table.delete())
            conn.execute(table.insert(), add_list)
            logging.info('Made predictions for {0} IPs'.format(add_count))
        except SQLAlchemyError as e:
            raise CydimeDatabaseException(e.message)

    conn.close()
