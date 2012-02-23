#!/bin/bash

set -u

./decode_packed 2> log_packed

java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx256m -Xms256m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand output_packed -format nbest -ref reference.en -rps 4 -m BLEU 4 closest > output_packed.bleu

diff -u output_packed.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


