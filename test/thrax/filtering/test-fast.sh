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

# Tests that both fast and exact filtering of grammars to test files works.

set -u

# fast filtering
java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -f -v -g grammar.filtered.gz dev.hi-en.hi.1 > fast 2> fast.log
cat grammar.de | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -f -v input.de >> fast 2>> fast.log

diff -u fast.log fast.log.gold > diff.fast

if [[ $? -eq 0 ]]; then
  rm -rf fast fast.log diff.fast
  exit 0
else
  exit 1
fi
