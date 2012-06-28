#!/bin/bash

# This script tests the Joshua pipeline, training on a 1000-sentence Urdu-English parallel corpus,
# and tuning and testing on 100-sentence test sets with four references.  It uses the Berkeley
# aligner for alignment to avoid the dependency on compiling GIZA.

$JOSHUA/scripts/training/pipeline.pl \
    --corpus input/train \
    --tune input/tune    \
    --test input/devtest \
    --source ur          \
    --target en          \
    --aligner berkeley   

