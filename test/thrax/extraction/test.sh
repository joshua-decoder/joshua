#!/bin/bash

# Tests that Hadoop works, in standalone mode.

set -u

rm -rf thrax.log grammar.gz .grammar.crc thrax

hadoop_dl_url=http://archive.apache.org/dist/hadoop/core/hadoop-0.20.2/hadoop-0.20.2.tar.gz
[[ ! -f $JOSHUA/lib/hadoop-0.20.2.tar.gz ]] && wget -q -O $JOSHUA/lib/hadoop-0.20.2.tar.gz $hadoop_dl_url
[[ ! -d hadoop-0.20.2 ]] && tar xzf $JOSHUA/lib/hadoop-0.20.2.tar.gz

unset HADOOP HADOOP_HOME HADOOP_CONF_DIR
export HADOOP=$(pwd)/hadoop-0.20.2

# run hadoop
$HADOOP/bin/hadoop jar $JOSHUA/thrax/bin/thrax.jar input/thrax.conf thrax > thrax.log 2>&1 
$HADOOP/bin/hadoop fs -getmerge thrax/final grammar.gz

size=$(perl -e "print +(stat('grammar.gz'))[7] . $/")

rm -rf hadoop-0.20.2
if [[ $size -eq 989817 ]]; then
  echo PASSED
  rm -rf thrax.log grammar.gz .grammar.crc thrax
  exit 0
else
  echo FAILED
  exit 1
fi
