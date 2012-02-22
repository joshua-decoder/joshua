#!/opt/local/bin/python

import os, sys, codecs

def main():
  for line in sys.stdin:
    (lhs, src, tgt, feature_string) = line.lstrip().rstrip().split(" ||| ")
    features = feature_string.split()
    print(lhs + " ||| " +
      src + " ||| " +
      tgt + " |||" +
      " p(e|f)=" + features[0] +
      " lex(e|f)=" + features[1] +
      " lex(f|e)=" + features[2])
        
if __name__ == "__main__":
    main()
