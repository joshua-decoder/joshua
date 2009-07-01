#!/usr/bin/env python

import os, sys, codecs

def main():
  chunkSize = int(sys.argv[1])
  fileNames = sys.argv[2:]
  
  inputFiles = []
  outputFiles = []
  
  for fileName in fileNames:
    inputFiles.append(codecs.open(fileName, "r", "utf-8"))
  
  ongoing = True
  numLines = 0
  numChunks = 0
  
  while (ongoing):
    if (numLines % chunkSize == 0):
      chunkName = "./chunk_" + ('%03d' % numChunks)  + "/"
      if not os.path.isdir(chunkName):
        os.mkdir(chunkName)
      numChunks += 1
      
      outputFiles = []
      for fileName in fileNames:
        outputFiles.append(codecs.open(chunkName + fileName, "w", "utf-8"))
    
    for inFile, outFile in zip(inputFiles, outputFiles):
      line = inFile.readline()
      
      if (line == ""):
        ongoing = False
        break
      outFile.write(line)
    numLines += 1
      
  print str(numLines) + " lines, " + str(numChunks) + " chunks"
  
    
if __name__ == "__main__":
    main()
