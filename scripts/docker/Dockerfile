FROM centos:latest
MAINTAINER Maheedhar Gunasekharan <mahee@iastate.edu>
RUN /bin/echo "Installing basic softwares and tools required "
RUN yum update -y
RUN yum install -y tar wget make gcc gcc-c++ autoconf automake rpm-build lzo gnutls-devel lzo-devel zlib-devel c-ares c-ares-devel glib* flex bison byacc bzip2 net-tools tcpdump net-tools tcpdump git ant
WORKDIR /opt
ENV JAVA_LINK http://download.oracle.com/otn-pub/java/jdk/7u65-b17/jdk-7u65-linux-x64.tar.gz
ENV JAVA_TAR jdk-7u65-linux-x64.tar.gz
ENV JAVA_DIR jdk1.7.0_65
RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" $JAVA_LINK
RUN tar xvf $JAVA_TAR
RUN chown -R root: $JAVA_DIR
RUN alternatives --install /usr/bin/java java /opt/$JAVA_DIR/bin/java 1
RUN alternatives --install /usr/bin/javac javac /opt/$JAVA_DIR/bin/javac 1
RUN alternatives --install /usr/bin/jar jar /opt/$JAVA_DIR/bin/jar 1
RUN /bin/echo "Installing SILK and its pre-requisites"
WORKDIR /tmp/
ENV LIBPCAP_VERSION 1.7.3
RUN wget http://www.tcpdump.org/release/libpcap-$LIBPCAP_VERSION.tar.gz
RUN tar -zxvf libpcap-$LIBPCAP_VERSION.tar.gz
WORKDIR libpcap-$LIBPCAP_VERSION
RUN ./configure && make && make install
WORKDIR /tmp/
ENV LIBFIXBUF_VERSION 1.6.2
RUN wget http://tools.netsa.cert.org/releases/libfixbuf-$LIBFIXBUF_VERSION.tar.gz
RUN tar -zxvf libfixbuf-$LIBFIXBUF_VERSION.tar.gz
WORKDIR libfixbuf-$LIBFIXBUF_VERSION
RUN ./configure && make && make install
WORKDIR /tmp/
ENV YAF_VERSION 2.7.1
RUN wget http://tools.netsa.cert.org/releases/yaf-$YAF_VERSION.tar.gz
RUN tar -zxvf yaf-$YAF_VERSION.tar.gz
WORKDIR yaf-$YAF_VERSION
RUN export PKG_CONFIG_PATH=/usr/local/lib/pkgconfig && ./configure && make && make install
WORKDIR /tmp/
ENV NETSA_VERSION 1.4.3
RUN wget http://tools.netsa.cert.org/releases/netsa-python-$NETSA_VERSION.tar.gz
RUN tar -zxvf netsa-python-$NETSA_VERSION.tar.gz
WORKDIR netsa-python-$NETSA_VERSION
RUN python setup.py build
RUN python setup.py install
WORKDIR /tmp/
ENV IPA_VERSION 0.5.2
RUN wget http://tools.netsa.cert.org/releases/ipa-$IPA_VERSION.tar.gz
RUN tar -zxvf ipa-$IPA_VERSION.tar.gz
WORKDIR ipa-$IPA_VERSION
RUN ./configure && make && make install
WORKDIR /
RUN mkdir silk
WORKDIR /silk
ENV SILK_VERSION 3.10.2
RUN wget http://tools.netsa.cert.org/releases/silk-$SILK_VERSION.tar.gz
RUN tar -zxvf silk-$SILK_VERSION.tar.gz
ENV SILK_DATA_ROOTDIR /silk/data/
VOLUME $SILK_DATA_ROOTDIR
WORKDIR /silk/silk-$SILK_VERSION
RUN ./configure --enable-data-rootdir=$SILK_DATA_ROOTDIR --enable-ipv6 --enable-output-compression=zlib --with-libfixbuf=/usr/local/lib/pkgconfig/ --with-libipa=/usr/local/lib/pkgconfig/
RUN make && make install
RUN /bin/echo "Installing Apache Web Server and its pre-requisites"
WORKDIR /tmp/
ENV APACHE_HTTP_VERSION 2.4.12
RUN wget http://apache.cs.utah.edu//httpd/httpd-$APACHE_HTTP_VERSION.tar.gz
RUN tar -zxvf httpd-$APACHE_HTTP_VERSION.tar.gz
WORKDIR httpd-$APACHE_HTTP_VERSION/srclib/
ENV APACHE_APR_VERSION 1.5.2
RUN wget http://mirror.olnevhost.net/pub/apache//apr/apr-$APACHE_APR_VERSION.tar.gz
RUN tar -zxvf apr-$APACHE_APR_VERSION.tar.gz
RUN mv apr-$APACHE_APR_VERSION ./apr/
ENV APACHE_APR_UTIL_VERSION 1.5.4
RUN wget http://apache.osuosl.org//apr/apr-util-$APACHE_APR_UTIL_VERSION.tar.gz
RUN tar -zxvf apr-util-$APACHE_APR_UTIL_VERSION.tar.gz
RUN mv apr-util-$APACHE_APR_UTIL_VERSION ./apr-util/
ENV PCRE_VERSION 8.36
RUN wget http://downloads.sourceforge.net/project/pcre/pcre/$PCRE_VERSION/pcre-$PCRE_VERSION.tar.bz2
RUN tar xvfj pcre-$PCRE_VERSION.tar.bz2
WORKDIR pcre-$PCRE_VERSION
RUN ./configure --prefix=/usr/local/pcre
RUN make && make install
WORKDIR /tmp/httpd-$APACHE_HTTP_VERSION/
RUN ./configure --with-pcre=/usr/local/pcre --with-included-apr
RUN make && make install
RUN /bin/echo "Installing Cydime"
RUN mkdir -p /cydime_data/{auth,bin,conf,data,logs}
RUN mkdir -p /repos/Cydime/
ENV CYDIME_REPO https://github.com/Ames-Laboratory-Cyber-Group/Cydime.git
RUN git clone $CYDIME_REPO /repos/Cydime/
WORKDIR /repos/Cydime/
ENV CYDIME_BRANCH ranker
RUN git checkout -b $CYDIME_BRANCH
RUN git pull origin $CYDIME_BRANCH
RUN rm -rf /tmp/*.tar.gz /tmp/httpd-$APACHE_HTTP_VERSION/srclib/*.tar.gz /tmp/httpd-$APACHE_HTTP_VERSION/srclib/*.bz2 /silk/*.tar.gz /opt/*.tar.gz