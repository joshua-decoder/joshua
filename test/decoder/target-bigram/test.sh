#!/bin/bash

(echo "this is a test" | $JOSHUA/bin/joshua-decoder -feature-function "TargetBigram -vocab vocab -top-n 2";
echo "this is a test" | $JOSHUA/bin/joshua-decoder -feature-function "TargetBigram -vocab vocab -top-n 3 -threshold 20";
echo "this is a test" | $JOSHUA/bin/joshua-decoder -feature-function "TargetBigram -vocab vocab -threshold 10") 2>log > out

# Compare
diff -u out out.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff out log
  exit 0
else
  exit 1
fi


