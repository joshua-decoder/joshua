#!/bin/bash

set -u

cat input | $JOSHUA/bin/decoder -config joshua.config -feature_function "FragmentLM -lm fragments.txt -build-depth 1" -fragment-map mapping.txt > output 2> log

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
	rm -f diff log output output.scores
	exit 0
else
	exit 1
fi
