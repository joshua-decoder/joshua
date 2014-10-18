#!/bin/bash

set -u

cat input.txt | $JOSHUA/bin/joshua-decoder -m 1g -threads 1 -c config > output 2> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
	rm -f diff log output output.scores
	exit 0
else
	exit 1
fi
