#!/bin/bash

# Strings together the preprocessing scripts

set -u

lang=$1

$JOSHUA/scripts/training/normalize-punctuation.pl $lang | $JOSHUA/scripts/training/penn-treebank-tokenizer.perl -l $lang | $JOSHUA/scripts/lowercase.perl
