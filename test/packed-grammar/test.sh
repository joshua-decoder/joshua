#!/bin/bash

set -u

# pack the grammar
$JOSHUA/scripts/support/grammar-packer.pl grammar.gz grammar.packed 2> packer.log

# decode
./decoder_command 2> log

java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx256m -Xms256m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand output -format nbest -ref reference.en -rps 4 -m BLEU 4 closest > output.bleu

diff -u output.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	rm -f diff log output.bleu output 
	rm -rf grammar.packed
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


