#!/bin/bash

# Trains a mallet model on source-annotated data of the form
#
# source_word target_word feat:val feat:val feat:val

if [[ -z $2 ]]; then
  echo "Usage: train-mallet.sh DATA_FILE MODEL_FILE"
  echo "This will read data from DATA_FILE and serialize the models to MODEL_FILE"
  exit
fi

java -mx16g -cp $JOSHUA/lib/mallet-2.0.7.jar:$JOSHUA/lib/trove4j-2.0.2.jar:$JOSHUA/class joshua.decoder.ff.LexicalSharpener $1 $2
