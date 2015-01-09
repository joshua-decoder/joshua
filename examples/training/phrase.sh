#!/bin/bash

# Build a standard, unlexicalized phrase-based model. This will be very fast. Moses
# is needed to learn the phrase table.

. ~/.bashrc
set -u

#export MOSES=/path/to/moses/version/2.1.1

$JOSHUA/bin/pipeline.pl \
    --rundir 4 \
    --readme "Baseline phrase model" \
    --type phrase \
    --source es \
    --target en \
    --corpus $JOSHUA/examples/data/corpus/asr/fisher_train \
    --corpus $JOSHUA/examples/data/corpus/asr/callhome_train \
    --tune $JOSHUA/examples/data/corpus/asr/fisher_dev \
    --test $JOSHUA/examples/data/corpus/asr/fisher_dev2 \
    --threads 8 \
    --tuner mert \
    --joshua-mem 5g \
    --packer-mem 8g \
    --optimizer-runs 5
