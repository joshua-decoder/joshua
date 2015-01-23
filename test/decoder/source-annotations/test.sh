#!/bin/bash

set -u

cat input.txt | $JOSHUA/bin/joshua-decoder -threads 1 -c joshua.config > output 2> log
cat input.txt | $JOSHUA/bin/joshua-decoder -threads 1 -c joshua.config -source-annotations >> output 2>> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log output.scores
  exit 0
else
  exit 1
fi
