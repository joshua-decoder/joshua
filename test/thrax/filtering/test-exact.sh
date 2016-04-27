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

# exact filtering
gzip -cd grammar.filtered.gz | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -v -e dev.hi-en.hi.1 > exact 2> exact.log
java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -v -e -g grammar.de input.de >> exact 2>> exact.log

diff -u exact.log exact.log.gold > diff.exact

if [[ $? -eq 0 ]]; then
  rm -rf exact exact.log diff.exact
  exit 0
else
  exit 1
fi
