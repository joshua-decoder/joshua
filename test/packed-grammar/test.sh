#!/bin/bash

set -u

export THRAX=$JOSHUA/thrax

# pack the grammar
rm -rf grammar.packed
$JOSHUA/scripts/support/grammar-packer.pl grammar.gz grammar.packed 2> packer.log

# generate the glue grammar
java -Xmx2g -cp $JOSHUA/lib/*:$THRAX/bin/thrax.jar edu.jhu.thrax.util.CreateGlueGrammar grammar.packed > grammar.glue 2> glue.log

# decode
cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
	echo PASSED
	rm -f packer.log diff log output.bleu output grammar.glue glue.log
	rm -rf grammar.packed
	exit 0
else
	echo FAILED
	exit 1
fi
