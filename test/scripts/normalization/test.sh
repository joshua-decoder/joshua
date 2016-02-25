#!/bin/bash

set -u

cat data/train.en | $JOSHUA/scripts/preparation/normalize.pl en > output
diff -U 1 output data/train.en.norm > diff

if [[ $? -eq 0 ]]; then
	rm -f output diff
	exit 0
else
	exit 1
fi
