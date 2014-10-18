#!/bin/bash

set -u

cat input | $JOSHUA/bin/joshua-decoder -c parse.config > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -rf output diff log
  exit 0
else
  exit 1
fi
