#!/bin/bash

set -u

# pack the grammar
rm -rf grammar.packed
$JOSHUA/scripts/support/grammar-packer.pl -v -g grammar.gz -o grammar.packed 2> packer.log

# generate the glue grammar
java -Xmx2g -cp $JOSHUA/lib/args4j-2.0.29.jar:$JOSHUA/class joshua.decoder.ff.tm.CreateGlueGrammar -g grammar.packed > grammar.glue 2> glue.log

# decode
cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua.config > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
	#rm -f packer.log diff log output.bleu output grammar.glue glue.log
	rm -rf grammar.packed
	exit 0
else
	exit 1
fi
