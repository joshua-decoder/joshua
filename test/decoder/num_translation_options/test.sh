#!/bin/bash

set -u

cat input | $JOSHUA/bin/joshua-decoder -c joshua.config > output 2> log
cat input | $JOSHUA/bin/joshua-decoder -c joshua.config -no-dot-chart >> output 2>> log
cat input | $JOSHUA/bin/joshua-decoder -c joshua.config.packed >> output 2>> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff log output output.scores
  exit 0
else
  exit 1
fi
