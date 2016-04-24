#!/bin/bash

set -u

(
echo -e "ELLA" | $JOSHUA/bin/joshua-decoder -config config
echo -e "Ella" | $JOSHUA/bin/joshua-decoder -config config -lowercase
echo -e "ELLA" | $JOSHUA/bin/joshua-decoder -config config -lowercase
) > output 2> .log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
    rm -f log output diff
    exit 0
else
    exit 1
fi
