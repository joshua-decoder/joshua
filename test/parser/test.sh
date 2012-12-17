#!/bin/bash

set -u

cat input | $JOSHUA/bin/joshua-decoder -c parse.config > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  echo PASSED
  rm -rf output diff log
  exit 0
else
  echo FAILED
  tail diff
  exit 1
fi


