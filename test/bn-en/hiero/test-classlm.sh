#!/bin/bash

set -u

cat input.bn | $JOSHUA/bin/joshua-decoder -c joshua-classlm.config > output 2> log

# Compare
diff -u output output-classlm.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log output.scores
  exit 0
else
  exit 1
fi


