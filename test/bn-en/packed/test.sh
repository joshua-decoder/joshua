#!/bin/bash

set -u

cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config > output 2> log

# Extract the translations and model scores
cat output | awk -F\| '{print $4 " ||| " $10}' > output.scores

# Compare
diff -u output.scores output.scores.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log output.scores
  exit 0
else
  exit 1
fi


