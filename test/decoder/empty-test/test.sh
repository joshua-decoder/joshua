#!/bin/bash

set -u

cat input | $JOSHUA/bin/joshua-decoder -mark-oovs true > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
    rm -f log output diff
    exit 0
else
    exit 1
fi
