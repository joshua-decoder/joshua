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
if [ $# -ne 3 ]
then
  echo "Usage: postprocessSAMT.sh mergedrules.gz samt.tm.gz samt.glue.gz"
  exit 2
fi

if [ ! -r $1 ]
then
  echo "Error: file $1 does not exist or is not readable."
  exit 3
fi

zgrep -v COUNT $1 | gzip > $2
zgrep COUNT $1 | awk 'BEGIN { FS="#" } ; { print $3 "#@1#@GOAL#1 0 0 0 0 0 0 0";\
	 print "@GOAL " $3 "#@1 @2#@GOAL#1 0 0 0 0.434294482 0 0 0" }' | gzip > $3

