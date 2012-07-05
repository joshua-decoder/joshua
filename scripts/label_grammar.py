#!/usr/bin/env python

import os, sys, codecs

def main():
  max_size = 0
  for line in sys.stdin:
    (lhs, src, tgt, feature_string) = line.lstrip().rstrip().split(" ||| ")
    features = feature_string.split()
    rule_string = lhs + " ||| " + src + " ||| " + tgt + " |||"
    
    max_size = max(max_size, len(features))
    
    i = 0
    for f in features:
      rule_string += " " + str(i) + "=" + f
      i += 1
    print rule_string

  dense_map = codecs.open("dense_map", "w", "utf-8")
  for i in xrange(max_size):
    dense_map.write(str(i) + "\t" + str(i) + "\n")
  dense_map.close()
        
  sys.stderr.write("Writing dense map file to file 'dense_map'\n")

if __name__ == "__main__":
    main()
