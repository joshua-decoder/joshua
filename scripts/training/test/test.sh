#!/bin/bash

$JOSHUA/scripts/training/pipeline.pl --corpus data/train --tune data/tune --test data/devtest --source ur --target en --aligner giza
