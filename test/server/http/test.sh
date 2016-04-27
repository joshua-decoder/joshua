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

# This test case starts a server and then throws 10 threads at it to make sure threading is working.

$JOSHUA/bin/decoder -threads 4 -server-port 9010 -server-type http -mark-oovs true > server.log 2>&1 &
serverpid=$!
sleep 1

curl -s http://localhost:9010/?q=I%20love%20it%20when%20I%20get%20the%20house%20clean%20before%20the%20weekend > output

kill -15 $serverpid 2> /dev/null

diff -u output expected > diff

if [[ $? -eq 0 ]]; then
  rm -f server.log output log diff
  exit 0
else
  exit 1
fi
