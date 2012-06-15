#!/bin/bash

set -u

cat data/train.ml | $JOSHUA/scripts/training/penn-treebank-tokenizer.perl -q -l ml > output 2> /dev/null
diff -u output data/train.ml.tok > diff

if [[ $? -eq 0 ]]; then
	echo PASSED
	exit 0
else
	echo FAILED TOKENIZATION TEST
	cat diff
	exit 1
fi

