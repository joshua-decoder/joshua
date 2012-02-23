#!/bin/bash

set -u

./decode_default 2> log_default

java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand output_default -format nbest -ref reference.en -rps 4 -m BLEU 4 closest > output_default.bleu

diff -u output_default.bleu output.gold.bleu > diff

if [ $? -eq 0 ]; then
	echo PASSED
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi


