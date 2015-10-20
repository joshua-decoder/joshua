#!/bin/bash

(for file in lm lm.gz lm.berkeleylm lm.berkeleylm.gz; do
    echo the chat-rooms | $JOSHUA/bin/joshua-decoder -feature-function "LanguageModel -lm_type berkeleylm -lm_order 2 -lm_file $file" -v 0 -output-format %f 2> log
done) > output

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log
  exit 0
else
  exit 1
fi
