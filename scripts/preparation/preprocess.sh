#!/bin/bash

# Strings together the preprocessing scripts

set -u

lang=$1

$JOSHUA/scripts/preparation/normalize.pl $lang | $JOSHUA/scripts/preparation/tokenize.pl -l $lang | $JOSHUA/scripts/preparation/lowercase.pl
