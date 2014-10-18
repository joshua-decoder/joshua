#!/bin/bash

# This script tests the Joshua pipeline, training on a 1000-sentence Urdu-English parallel corpus,
# and tuning and testing on 100-sentence test sets with four references.  It uses the Berkeley
# aligner for alignment to avoid the dependency on compiling GIZA.

$JOSHUA/scripts/training/pipeline.pl \
    --rundir 1                 \
    --source ur                \
    --target en                \
    --corpus input/train       \
    --tune input/tune          \
    --test input/devtest       \
    --lm-order 3               \
    --aligner berkeley > pipeline.log 2>&1

#diff -u 1/test/final-bleu final-bleu.gold

if [[ -e "1/test/final-bleu" ]]; then
	exit 0
else
	exit 1
fi
