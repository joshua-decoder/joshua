#!/bin/bash

# Tests that both fast and exact filtering of grammars to test files works.

set -u

# fast filtering
java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -f -v -g grammar.filtered.gz dev.hi-en.hi.1 > fast 2> fast.log
cat grammar.de | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter -f -v input.de >> fast 2>> fast.log

diff -u fast.log fast.log.gold > diff.fast

if [[ $? -eq 0 ]]; then
  rm -rf fast fast.log diff.fast
  exit 0
else
  exit 1
fi
