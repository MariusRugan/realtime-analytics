#!/bin/bash

export JETSTREAM_HOME=$(pwd)

export JETSTREAM_NETMASK="10.28.5.255"

export JETSTREAM_JAVA_OPTS="-server -Xms2g -Xmx2g -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+UseCompressedOops -X:MaxTenuringThreshold=8 -XX:CMSInitiatingOccupancyFraction=75 -XX:MaxNewSize=1g -XX:NewSize=1g -XX:+CMSConcurrentMTEnabled -XX:+CMSScavengeBeforeRemark"

export JETSTREAM_ZKSERVER_HOST=zkserver
export JETSTREAM_ZKSERVER_PORT=2181

export JETSTREAM_MONGOURL=mongo://mongoserver:27017/config
export MONGO_HOME=$JETSTREAM_MONGOURL

export PULSAR_KAFKA_BROKERS="kafkaserver:9092"
export PULSAR_KAFKA_ZK="kafkaserver:2181"

export PULSAR_CASSANDRA="cassandraserver"