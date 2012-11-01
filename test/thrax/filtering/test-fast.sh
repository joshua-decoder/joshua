#!/bin/bash

# Tests that both fast and exact filtering of grammars to test files works.

set -u

# fast filtering
gzip -cd grammar.filtered.gz | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/thrax/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -f -v dev.hi-en.hi.1 > fast 2> fast.log

diff -u fast.log fast.log.gold > diff.fast

if [[ $? -eq 0 ]]; then
  echo PASSED
  rm -rf fast fast.log diff.fast
  exit 0
else
  echo FAILED
  cat diff.fast
  exit 1
fi
