#!/usr/bin/env python

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
