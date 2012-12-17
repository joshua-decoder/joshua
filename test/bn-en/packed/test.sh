#!/bin/bash

set -u

cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config > output 2> log

$JOSHUA/bin/bleu output reference.en 4 > output.bleu

diff -u output.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
  rm -f output log diff
	exit 0
else
	echo FAILED
	tail diff
	exit 1
fi


