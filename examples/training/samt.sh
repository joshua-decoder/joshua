#!/bin/bash

# Build a baseline SAMT model. This is just a matter of changing the type,
# and then increasing the amount of RAM a bit

. ~/.bashrc
set -u

# Moses is not needed for building Hiero models; the grammar extraction is done with Thrax.

$JOSHUA/bin/pipeline.pl \
    --rundir 3 \
    --readme "Baseline SAMT model" \
    --type samt \
    --source es \
    --target en \
    --corpus $JOSHUA/examples/data/corpus/asr/fisher_train \
    --corpus $JOSHUA/examples/data/corpus/asr/callhome_train \
    --tune $JOSHUA/examples/data/corpus/asr/fisher_dev \
    --test $JOSHUA/examples/data/corpus/asr/fisher_dev2 \
    --threads 4 \
    --tuner mert \
    --joshua-mem 20g \
    --packer-mem 8g \
    --optimizer-runs 5
