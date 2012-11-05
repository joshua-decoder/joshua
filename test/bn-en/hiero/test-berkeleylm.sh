#!/bin/bash

set -u

cat input.bn | $JOSHUA/bin/joshua-decoder -m 1g -threads 2 -c joshua-berkeleylm.config > output 2> log

# Extract the translations and model scores
cat output | awk -F\| '{print $4 " ||| " $10}' > output.scores

# Compare
diff -u output.scores gold.scores.berkeleylm > diff

if [ $? -eq 0 ]; then
	echo PASSED
	rm -f output log output.scores diff
	exit 0
else
	echo FAILED
	tail diff
	exit 1
fi


