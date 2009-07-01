#!/usr/bin/env python

import os, sys, codecs

def main():
  fileName = sys.argv[1]
  
  tgsName = fileName + ".giza.tgs"
  sgtName = fileName + ".giza.sgt"
  
  inFile = codecs.open(fileName, "r", "utf-8")
  tgsFile = codecs.open(tgsName, "w", "utf-8")
  sgtFile = codecs.open(sgtName, "w", "utf-8")
  
  for line in inFile:
    (source, target, tgs, sgt) = line.rstrip().split(u" ")
    tgsFile.write(tgs + u" " + source + u" " + target + u" " + tgs + u"\n")
    sgtFile.write(sgt + u" " + target + u" " + source + u" " + sgt + u"\n")
    
if __name__ == "__main__":
    main()
