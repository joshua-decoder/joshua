#!/bin/bash

set -u

./decoder_command 2> log

java -cp $JOSHUA/bin -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand <(perl -pe 's/_OOV//' output) -format nbest -ref reference.en -rps 4 -m BLEU 4 closest > output.bleu

diff -u output.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


