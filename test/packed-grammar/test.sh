#!/bin/bash

set -u

export THRAX=$JOSHUA/thrax

# pack the grammar
rm -rf grammar.packed
$JOSHUA/scripts/support/grammar-packer.pl grammar.gz grammar.packed 2> packer.log

# generate the glue grammar
$JOSHUA/thrax/scripts/create_glue_grammar.sh grammar.packed > grammar.glue

# decode
cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config > output 2> log

$JOSHUA/bin/bleu output reference.en 4 > output.bleu

diff -u output.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	rm -f packer.log diff log output.bleu output grammar.glue
	rm -rf grammar.packed
	exit 0
else
	echo FAILED
	tail diff
	exit 1
fi


