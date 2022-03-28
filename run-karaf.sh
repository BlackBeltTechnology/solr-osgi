#!/bin/bash

export EXTRA_JAVA_OPTS=-Dfile.encoding=UTF-8

CURR_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

KARAF_DIR=$CURR_DIR/.karaf

RUN_DIR=/tmp/.karaf
rm -rf $RUN_DIR
mkdir $RUN_DIR
mkdir $KARAF_DIR
mkdir $KARAF_DIR/etc
mkdir $KARAF_DIR/deploy

cp -R $CURR_DIR/solr-osgi-karaf/target/assembly/etc/ $CURR_DIR/.karaf/
cp -R $CURR_DIR/solr-osgi-karaf/target/assembly/etc/ $CURR_DIR/.karaf/etc
cp -R $CURR_DIR/override/etc $CURR_DIR/.karaf/
cp -R $CURR_DIR/override/etc $CURR_DIR/.karaf/etc


(export KARAF_DEBUG=true; export EXTRA_JAVA_OPTS="-Xmx512m -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dorg.jooq.no-logo=true"; export KARAF_BASE=$KARAF_DIR; cd $RUN_DIR && tar xvvzf $CURR_DIR/solr-osgi-karaf/target/solr-osgi-karaf-*.tar.gz && mv ./solr-osgi-karaf-* ./solr-osgi-karaf && ./solr-osgi-karaf/bin/karaf run clean debug)
