#!/bin/bash

# build this separately since the data here is too small for Kneser-Ney
if test $(uname -s) = "Darwin"; then
  $SRILM/bin/macosx/ngram-count -order 3 -text data/train.en -lm lm.gz
else
  $SRILM/bin/i686-m64/ngram-count -order 3 -text data/train.en -lm lm.gz
fi

if [[ ! -e lm.gz ]]; then
  echo "I wasn't able to build the LM file"
  echo "You might have to adjust the path to SRILM's ngram-count in this script"
  exit
fi

$JOSHUA/scripts/training/pipeline.pl --corpus data/train --tune data/tune --test data/devtest --source ur --target en --aligner giza --lmfile lm.gz
