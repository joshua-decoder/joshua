#!/bin/bash


# build this separately since the data here is too small for Kneser-Ney
$SRILM/bin/i686-m64/ngram-count -order 3 -text train/corpus.en -lm lm.gz

$JOSHUA/scripts/training/pipeline.pl --corpus data/train --tune data/tune --test data/devtest --source ur --target en --aligner giza --lmfile lm.gz
