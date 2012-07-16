#!/bin/bash

set -u

cat data/train.en | $JOSHUA/scripts/training/normalize-punctuation.pl en > output
diff -u output data/train.en.norm > diff

if [[ $? -eq 0 ]]; then
	echo PASSED
	exit 0
else
	echo FAILED NORMALIZATION TEST
	cat diff
	exit 1
fi

