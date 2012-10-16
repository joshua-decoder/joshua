#!/bin/bash

set -u

./decoder_command 2> log

# Extract the translations and model scores
cat output | awk -F\| '{print $4 " ||| " $10}' > output.scores

# Compare
diff output.scores gold.scores > diff

if [ $? -eq 0 ]; then
	echo PASSED
	rm -f diff log output output.scores
	exit 0
else
	echo FAILED
	cat diff
	exit 1
fi
