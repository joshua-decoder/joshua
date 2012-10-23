#!/bin/bash

# Tests that Hadoop works, in standalone mode.

set -u

[[ ! -d hadoop-0.20.2 ]] && tar xzf $JOSHUA/lib/hadoop-0.20.2.tar.gz

unset HADOOP HADOOP_HOME HADOOP_CONF_DIR
export HADOOP=$(pwd)/hadoop-0.20.2

# run hadoop
$HADOOP/bin/hadoop jar $JOSHUA/thrax/bin/thrax.jar thrax.conf thrax > thrax.log 2>&1 
$HADOOP/bin/hadoop fs -getmerge thrax/final grammar

if [[ $(uname -s) = "Darwin" ]]; then
  size=$(gstat -c"%s" grammar)
else
  size=$(stat -c"%s" grammar)
fi

if [[ $size -eq 6385751 ]]; then
  echo PASSED
  rm -rf thrax.log grammar thrax
  exit 0
else
  echo FAILED
  tail thrax.log
  exit 1
fi
