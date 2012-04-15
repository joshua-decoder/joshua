#!/usr/bin/env python

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
