#!/bin/bash

# This script tests the Joshua pipeline, training on a 1000-sentence Urdu-English parallel corpus,
# and tuning and testing on 100-sentence test sets with four references.  It uses the Berkeley
# aligner for alignment to avoid the dependency on compiling GIZA.

rm -rf 2
$JOSHUA/scripts/training/pipeline.pl \
    --readme "testing GHKM extraction" \
    --rundir 2                 \
    --type ghkm                \
    --source ur                \
    --target en                \
    --corpus input/train       \
    --last-step GRAMMAR        \
    --aligner-mem 4g           \
    --aligner berkeley > pipeline-ghkm.log 2>&1

#diff -u 1/test/final-bleu final-bleu.gold

size=$(perl -e "print +(stat('2/grammar.gz'))[7]")
if [[ $size -ne 0 ]]; then
	exit 0
else
	exit 1
fi

