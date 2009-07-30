#!/usr/bin/env python -u

import os, sys, codecs, re

def usage():
  print "Usage info for filterGrammar.py"
  print "  filterGrammar.py devset devset ..."
  print "Where:"
  print "  stdin    - pipe in SAMT grammar to filter"
  print "  stdout   - pipes out filtered SAMT grammar"
  print "  devset   - plain text dev or test file"
  print
  sys.exit()

def main():
  arguments = sys.argv[1:]
  
  vocabulary = {}
  
  for devFileName in arguments:
    devFile = codecs.open(devFileName, "r", "utf-8").read().split(u"\n")
    for line in devFile:
      line = line.rstrip().lstrip()
      line = re.sub("[ ]+", " ", line)
      for word in line.split(" "):
        vocabulary[word] = 1 
  
  
  for line in sys.stdin:
    (source, sep, rule) = line.rstrip().partition("#")
    sourceWords = source.split(" ")
    
    skip = False
    for word in sourceWords:
      if ((not re.match("^@[^ #a-z]+$", word)) and (not word in vocabulary)):
        skip = True
        break
    if skip:
      continue
    else:
      sys.stdout.write(line)
    
if __name__ == "__main__":
    main()
