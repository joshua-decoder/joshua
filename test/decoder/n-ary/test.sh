#!/bin/bash

set -u

cat input.txt | $JOSHUA/bin/joshua-decoder -m 1g -threads 1 -c joshua.config > output 2> log

# Extract the translations and model scores
cat output | awk -F\| '{print $4 " ||| " $10}' > output.scores

# Compare
diff -u output.scores gold.scores > diff

if [ $? -eq 0 ]; then
	rm -f diff log output output.scores
	exit 0
else
	exit 1
fi
