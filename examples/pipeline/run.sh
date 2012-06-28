#!/bin/bash

# This script tests the Joshua pipeline, training on a 1000-sentence Urdu-English parallel corpus,
# and tuning and testing on 100-sentence test sets with four references.  It uses the Berkeley
# aligner for alignment to avoid the dependency on compiling GIZA.  We place the output in a run
# directory named "1" for easy cleanup.

$JOSHUA/scripts/training/pipeline.pl \
    --rundir 1                 \
    --source ur                \
    --target en                \
    --corpus input/train       \
    --tune input/tune          \
    --test input/devtest       \
    --aligner berkeley

