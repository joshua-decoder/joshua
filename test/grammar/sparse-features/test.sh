#!/bin/bash

set -u

echo el chico | $JOSHUA/bin/decoder -c joshua.config -v 0 > output 2> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log
  exit 0
else
  exit 1
fi


