#!/bin/bash

# Tests dynamic sentence-level filtering.

set -u

cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config -filter-grammar > output.filter 2> log.filter

# Extract the translations and model scores
cat output.filter | awk -F\| '{print $4 " ||| " $10}' > output.scores

# Compare
diff -u output.scores output.scores.gold > diff

if [ $? -eq 0 ]; then
  rm -rf output.scores diff output.filter log.filter
  exit 0
else
  exit 1
fi
