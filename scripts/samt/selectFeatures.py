#!/usr/bin/env python -u
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

import os, sys, codecs

def usage():
  print "Usage info for selectFeatures.py"
  print "  selectFeatures.py index index ..."
  print "Where:"
  print "  stdin    - pipe in SAMT filtered grammar"
  print "  stdout   - pipes out SAMT filtered grammar"
  print "  index    - plain feature index (i.e. '2') or range (i.e. '0-4')"
  print
  sys.exit()

def main():
  arguments = sys.argv[1:]
  
  featureIndices = []
  
  for i in arguments:
    try:
      index = int(i)
      featureIndices.append(index)
    except ValueError:
      try:
        bounds = i.split("-")
        
        start = int(bounds[0])
        end = int(bounds[1])
        featureIndices.extend(range(start, end+1))
      except:
        print "[ERR]    Wrong argument formatting.\n"
        usage()
  
  out = []
  
  for line in sys.stdin:
    (rule, sep, featureString) = line.rstrip().rpartition("#")
    
    featureScores = featureString.split(" ")
    
    filteredScores = []
    for i in featureIndices:
      filteredScores.append(featureScores[i])
    
    sys.stdout.write(rule + '#' + (" ".join(filteredScores)) + "\n")
    
    
if __name__ == "__main__":
    main()
