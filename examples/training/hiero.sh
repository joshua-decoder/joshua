#!/bin/bash

. ~/.bashrc
set -u

# Moses is not needed for building Hiero models; the grammar extraction is done with Thrax.

$JOSHUA/bin/pipeline.pl \
    --rundir 2 \
    --readme "Baseline Hiero model" \
    --type hiero \
    --source es \
    --target en \
    --corpus $JOSHUA/examples/data/corpus/asr/fisher_train \
    --corpus $JOSHUA/examples/data/corpus/asr/callhome_train \
    --tune $JOSHUA/examples/data/corpus/asr/fisher_dev \
    --test $JOSHUA/examples/data/corpus/asr/fisher_dev2 \
    --threads 8 \
    --tuner mert \
    --joshua-mem 10g \
    --packer-mem 8g \
    --optimizer-runs 5
