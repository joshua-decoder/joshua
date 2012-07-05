#!/bin/bash

set -u

cat input.bn | $JOSHUA/joshua-decoder -m 1g -threads 2 -c joshua-berkeleylm.config > output 2> log

java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx256m -Xms256m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand output -format nbest -ref reference.en -rps 4 -m BLEU 4 closest > output.bleu

diff -u output.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


