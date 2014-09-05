import logging

from sqlalchemy import Table, Column, BigInteger, Integer, Sequence, Date, String, Boolean
from sqlalchemy import create_engine, MetaData
from sqlalchemy.exc import SQLAlchemyError
from urllib import quote

#import Common as Com
import ScraperConfig as Conf

from Errors import CydimeScraperException

def get_db_engine():
    db_string = ''
    e = Conf.scraper_tmp_engine.lower()
    if e == 'sqlite':
        db_string = e + ':///' + Conf.scraper_tmp_location
    elif e == 'postgresql' or e == 'mysql':
        port = '' if Conf.scraper_db_port == 0 else ':' + Conf.scraper_tmp_port
        db_string = '{0}://{1}:{2}@{3}{4}'.format(e, Conf.scraper_tmp_user, 
                                                  quote(Conf.scraper_tmp_passwd),
                                                  Conf.scraper_tmp_host, port)
        db_string += '/' + Conf.scraper_tmp_name
    else:
        raise CydimeScraperException('Unsupported Database Type {0}'\
                                    .format(Conf.scraper_tmp_engine))

    try:
        engine = create_engine(db_string, echo=False)
        metadata = MetaData()

        tmp_table = Table(Conf.scraper_tmp_table, metadata,
                # BigInteger so we can support IPv6 (eventually)
                Column('ip_addr', BigInteger, Sequence('ip_addr_seq'),
                       primary_key=True),
                Column('ip_type', Integer, Sequence('ip_type_seq'), index=True),
        )

        scraper_table = Table(Conf.scraper_db_table, metadata,
                # BigInteger so we can support IPv6 (eventually)
                Column('ip_addr', BigInteger, Sequence('ip_addr_seq'),
                        primary_key=True),
                # last date we saw traffic
                Column('date', Date, nullable=True),
                # top service
                Column('top_serv', String(5), nullable=True),
                # host string - null if no results
                Column('host', String(100), nullable=True),
                # netname from whois - null if no results
                Column('whois', String(100), nullable=True),
        )

        metadata.create_all(engine)
    except SQLAlchemyError as e:
        raise

    return (engine, tmp_table, scraper_table)

if __name__ == '__main__':
   get_scraper_db_engine() 
