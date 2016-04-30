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

import os, sys, codecs

def usage():
  print "Usage info for lexprob2samt.py"
  print "  lexprob2samt.py lexfile"
  print "Where:"
  print "  lexfile  - Joshua lexprob file"
  print "  output   - lexfile.samt.{tgs,sgt}" 
  
  print
  sys.exit()

def main():
  if (len(sys.argv) == 1 or sys.argv[1] == "-h"):
    usage()
  
  fileName = sys.argv[1]
  
  tgsName = fileName + ".samt.tgs"
  sgtName = fileName + ".samt.sgt"
  
  inFile = codecs.open(fileName, "r", "utf-8")
  tgsFile = codecs.open(tgsName, "w", "utf-8")
  sgtFile = codecs.open(sgtName, "w", "utf-8")
  
  for line in inFile:
    (source, target, tgs, sgt) = line.rstrip().split(u" ")
    tgsFile.write(target + u" " + source + u" " + tgs + u"\n")
    sgtFile.write(source + u" " + target + u" " + sgt + u"\n")
    
if __name__ == "__main__":
    main()
