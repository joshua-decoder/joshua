#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
