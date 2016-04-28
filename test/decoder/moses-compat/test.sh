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
set -u

rm -f output
# should write translation to stdout, output-format info to n-best.txt
echo help | joshua -v 0 -moses -n-best-list n-best1.txt 10 distinct > output
# should write output-format info to n-best.txt (since no -moses)
echo help | joshua -v 0 -n-best-list n-best2.txt 10 distinct >> output
# should write translation to stdout
echo help | joshua -v 0 -moses >> output

echo >> output
echo "# n-best stuff to follow:" >> output
cat n-best1.txt n-best2.txt >> output

# Compare
diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
    rm -f diff log output n-best1.txt n-best2.txt
    exit 0
else
    exit 1
fi
