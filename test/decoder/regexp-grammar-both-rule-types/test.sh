#!/bin/bash

set -u

cat input | $JOSHUA/joshua-decoder -c config > output 2> /dev/null

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
	echo PASSED
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


