#!/bin/bash

# Creates a glue grammar from a regular grammar (packed or plain text).

if [[ -z "$1" ]]; then
  echo "Creates a glue grammar from a main grammar, writing to STDOUT."
  echo "Usage: $0 /path/to/main/grammar"
  exit
fi

java -Xmx2g -cp $JOSHUA/lib/args4j-2.0.29.jar:$JOSHUA/class joshua.decoder.ff.tm.CreateGlueGrammar -g $1
