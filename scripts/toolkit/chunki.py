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
import gzip

def usage():
  print "Usage info for chunki.py"
  print "  chunki.py size file file ..."
  print "Where:"
  print "  size   - chunk size"
  print "  file   - corpus file, plain UTF8, same number of lines each"
  print "  output - chunk_000/files" 
  
  print
  sys.exit()

def main():
  if (len(sys.argv) < 2 or sys.argv[1] == "-h"):
    usage()

  chunkSize = int(sys.argv[1])
  fileNames = sys.argv[2:]
  
  inputFiles = []
  outputFiles = []
  
  for fileName in fileNames:
    if (fileName.endswith(".gz")):
      inputFiles.append(codecs.getreader('utf-8')(gzip.open(fileName)))
    else:
      inputFiles.append(codecs.open(fileName, "r", "utf-8"))
  
  ongoing = True
  numLines = 0
  numChunks = 0
  
  while (ongoing):
    if (numLines % chunkSize == 0):
      chunkName = "./chunk_" + ('%03d' % numChunks)  + "/"
      if not os.path.isdir(chunkName):
        os.mkdir(chunkName)
      
      outputFiles = []
      for fileName in fileNames:
        outputFiles.append(codecs.open(chunkName + fileName, "w", "utf-8"))
      print "[INF]    Creating chunk " + ('%03d' % numChunks) + "."
      numChunks += 1
    
    for inFile, outFile in zip(inputFiles, outputFiles):
      line = inFile.readline()
      
      if (line == ""):
        ongoing = False
        break
      outFile.write(line)
    numLines += 1
      
  print "[END]    Done: " + str(numLines) + " lines, " + str(numChunks) + " chunks."
  
    
if __name__ == "__main__":
    main()
