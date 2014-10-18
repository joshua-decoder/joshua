#!/bin/bash

# Tests loose filtering.

set -u

# loose filtering
gzip -cd grammar.filtered.gz | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -v -l dev.hi-en.hi.1 > loose 2> loose.log
cat grammar.de | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -v -l input.de >> loose 2>> loose.log

diff -u loose.log loose.log.gold > diff.loose

if [[ $? -eq 0 ]]; then
  rm -rf loose loose.log diff.loose
  exit 0
else
  exit 1
fi
