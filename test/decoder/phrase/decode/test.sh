#!/bin/bash

set -u

cat corpus.es | $JOSHUA/bin/joshua-decoder -threads 1 -c config > output 2> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log
  exit 0
else
  exit 1
fi


