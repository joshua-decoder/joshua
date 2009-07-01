#!/usr/bin/env python

import os, sys, codecs

def main():
  delimiter = sys.argv[1]
  fileNames = sys.argv[2:]
  
  files = []
  
  for fileName in fileNames:
    files.append(codecs.open(fileName, "r", "utf-8"))
  
  ongoing = True
  
  while (ongoing):
    lines = []
    for file in files:
      line = file.readline()
      if (line == ""):
        ongoing = False
        break
      lines.append(line.rstrip())
    if (ongoing):
      print delimiter.join(lines)  

    
if __name__ == "__main__":
    main()
