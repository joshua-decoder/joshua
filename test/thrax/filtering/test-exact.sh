#!/bin/bash

# Tests that both fast and exact filtering of grammars to test files works.

set -u

# exact filtering
gzip -cd grammar.filtered.gz | java -Xmx500m -Dfile.encoding=utf8 -cp $JOSHUA/thrax/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -v dev.hi-en.hi.1 > exact 2> exact.log

diff -u exact.log exact.log.gold > diff.exact

if [[ $? -eq 0 ]]; then
  echo PASSED
  rm -rf exact exact.log diff.exact
  exit 0
else
  echo FAILED
  tail diff.exact
  exit 1
fi
