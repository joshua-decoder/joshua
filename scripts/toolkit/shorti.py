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
  print "Usage info for shorti.py"
  print "  shorti.py source target alignment [limit]"
  
  print
  sys.exit()

def main():
  if (len(sys.argv) < 4 or sys.argv[1] == "-h"):
    usage()

  limit = 99

  f = codecs.open(sys.argv[1], "r", "utf-8")
  e = codecs.open(sys.argv[2], "r", "utf-8")
  a = codecs.open(sys.argv[3], "r", "utf-8")
  
  if (len(sys.argv) >= 5):
    limit = int(sys.argv[4])
  
  f_out = codecs.open(sys.argv[1] + ".tr", "w", "utf-8")
  e_out = codecs.open(sys.argv[2] + ".tr", "w", "utf-8")
  a_out = codecs.open(sys.argv[3] + ".tr", "w", "utf-8")
  
  total = 0
  written = 0
  
  print "[INF]\tfiltering corpus, only sentences shorter than %d words" % (limit)
  
  while (True):
    f_line = f.readline()
    e_line = e.readline()
    a_line = a.readline()
    
    if ((a_line == "") or (f_line == "") or (e_line == "")):
      break

    f_line = f_line.rstrip().lstrip()
    e_line = e_line.rstrip().lstrip()
    a_line = a_line.rstrip().lstrip()
    
    oversized = False
    points = a_line.split()
    for point in points:
      coordinates = point.split("-")
      f_idx = int(coordinates[0])
      e_idx = int(coordinates[1])
      
      #print str(f_idx) + " - " + str(e_idx)
      
      if ((f_idx >= limit) or (e_idx >= limit)):
        oversized = True
        break
    
    if not oversized:
      written += 1
      
      f_out.write(f_line + "\n")
      e_out.write(e_line + "\n")
      a_out.write(a_line + "\n")
    total += 1
    
  print "[INF]\twritten %d sentences of %d (dropped %d)" % (written, total, total - written)


    
if __name__ == "__main__":
    main()
