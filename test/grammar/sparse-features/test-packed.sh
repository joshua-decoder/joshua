#!/bin/bash

set -u

echo el chico | $JOSHUA/bin/decoder -c joshua-packed.config -v 0 > output

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output
  exit 0
else
  exit 1
fi


