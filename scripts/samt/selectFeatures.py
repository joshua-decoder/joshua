#!/usr/bin/env python -u

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
