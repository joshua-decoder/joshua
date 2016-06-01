#!/bin/bash

# Trains a mallet model on source-annotated data of the form
#
# source_word target_word feat:val feat:val feat:val

if [[ -z $2 ]]; then
  echo "Usage: train-mallet.sh [-d train_data] [-m model] [-t test_data]"
  echo "If -d is given, a model will be trained, and written out if -m is given."
  echo "If -m is given without -t, the model will be loaded."
  echo "Use -t to specify a test file"
  exit
fi

LOG_PROPERTIES=$JOSHUA/lib/mallet.properties

java -mx16g -cp $JOSHUA/lib/mallet-2.0.7.jar:$JOSHUA/lib/trove4j-2.0.2.jar:$JOSHUA/lib/args4j-2.0.29.jar:$JOSHUA/class -Djava.util.logging.config.file=$LOG_PROPERTIES joshua.decoder.ff.LexicalSharpener "$@"
