#!/bin/bash

set -u

cat corpus.es | $JOSHUA/bin/joshua-decoder -threads 1 -c joshua.config > output 2> log

# Compare
num=$(sort -u output | wc -l)

if [ $num -eq 300 ]; then
  rm -f output log
  exit 0
else
  exit 1
fi


