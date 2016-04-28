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
# Runs all the test cases (scripts named test*.sh) beneath this directory, reporting success or
# failure.  Each test script should echo "PASSED" (and return 0) on success or "FAILED" (and return
# nonzero) on failure.
#
# Important!  Do not rename this script to match the pattern test*.sh, or it will execute
# recursively.

trap exit INT

GREEN='\033[01;32m'
RED='\033[01;31;31m'
NONE='\033[00m'

tests=$(find . -name test*.sh | perl -pe 's/\n/ /g')
echo "TESTS: $tests"

pass=0
fail=0
for file in $tests; do
  if [[ ! -x $file ]]; then
    continue;
  fi
  dir=$(dirname $file | sed 's/^\.\///')
  name=$(basename $file)
  echo -n "Running test '$name' in test/$dir..."
  pushd $dir > /dev/null
  bash $name
  if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}PASSED${NONE}"
    let pass=pass+1
  else
    echo -e "${RED}FAILED${NONE}"
    let fail=fail+1
  fi
  popd > /dev/null
done

echo -e "${GREEN}PASSED${NONE} $pass ${RED}FAILED${NONE} $fail"
