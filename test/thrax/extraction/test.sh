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

if [[ -z $HADOOP ]]; then
  exit 0
fi

$JOSHUA/scripts/training/run_thrax.py -f input/thrax.conf input/train.{ps,en,a} 2> thrax.log

size=$(perl -e "print +(stat('grammar.gz'))[7] . $/")

if [[ $size -eq 106851 ]]; then
  rm -rf thrax.log grammar.gz
  exit 0
else
  exit 1
fi
