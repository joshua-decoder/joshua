#!/usr/bin/env python
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

"""
Converts the words in a tokenized corpus into classes using the provided map.

Usage:

  $0 MAP INPUT_FILE OUTPUT_FILE

where the format of the map is 

  WORD CLASS
"""
 
import sys
 
classMap = {}
 
classFile = open(sys.argv[1])
input = open(sys.argv[2])
output = open(sys.argv[3], 'w+')
 
# First read classMap
for line in classFile:
  line = line.strip()
  lineComp = line.split()
  classMap[lineComp[0]] = lineComp[1]
 
# Now read corpus
for line in input:
  line = line.strip().lower()
  lineComp = line.split()
  translation = []
  for word in lineComp:
    if word in classMap:
      translation.append(classMap[word])
    else:
      translation.append("-1")
  output.write(" ".join(translation) + "\n")
 
classFile.close()
input.close()
output.close()
