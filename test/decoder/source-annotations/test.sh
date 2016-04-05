#!/bin/bash

# Tests the language model code that uses the source-side projection instead of the word itself.
# When translating a word, if there is a source-side annotation of the label "class", and
# -source-annotations was added to the invocation, the LM will use that source-side class instead
# of the translated word.

set -u

cat input.txt | $JOSHUA/bin/joshua-decoder -threads 1 -c joshua.config > output 2> log
cat input.txt | $JOSHUA/bin/joshua-decoder -threads 1 -c joshua.config -source-annotations >> output 2>> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log output.scores
  exit 0
else
  exit 1
fi
