#!/usr/bin/env python

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
