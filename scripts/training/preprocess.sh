#!/bin/bash

# Strings together the preprocessing scripts

set -u

LANG=$1

$JOSHUA/scripts/training/normalize-punctuation.pl $LANG | $JOSHUA/scripts/training/penn-treebank-tokenizer.perl -l $LANG | $JOSHUA/scripts/lowercase.perl
