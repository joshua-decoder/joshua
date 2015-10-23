#!/bin/bash

# Tests that Hadoop works, in standalone mode.

set -u

export VERSION=2.5.2

rm -rf thrax.log grammar.gz .grammar.crc thrax

[[ ! -d hadoop-$VERSION ]] && tar xzf $JOSHUA/lib/hadoop-$VERSION.tar.gz

unset HADOOP HADOOP_HOME HADOOP_CONF_DIR
export HADOOP=$(pwd)/hadoop-$VERSION

# run hadoop
$HADOOP/bin/hadoop jar $JOSHUA/thrax/bin/thrax.jar input/thrax.conf thrax > thrax.log 2>&1 
$HADOOP/bin/hadoop fs -getmerge thrax/final grammar.gz

size=$(perl -e "print +(stat('grammar.gz'))[7] . $/")

rm -rf hadoop-$VERSION
if [[ $size -eq 1004401 ]]; then
  rm -rf thrax.log grammar.gz .grammar.crc thrax
  exit 0
else
  exit 1
fi
