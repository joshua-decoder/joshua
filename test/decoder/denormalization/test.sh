#!/bin/bash

set -u

cat input.txt | $JOSHUA/bin/joshua-decoder -output-format "%S" -mark-oovs false -top-n 1 > output 2> log

# Compare
diff -u output output.expected > diff

if [ $? -eq 0 ]; then
	rm -f output log diff
	exit 0
else
	exit 1
fi
