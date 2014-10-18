#!/bin/bash

set -u

cat input | $JOSHUA/bin/joshua-decoder -m 1g -c config > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
    rm -f output log diff
    exit 0
else
    exit 1
fi
