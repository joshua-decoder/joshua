#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum

rm -f test.nbest test.1best

cat test.plf | $JOSHUA/bin/joshua-decoder -m 500m -c joshua.config > output 2> log

if [[ $? -ne 0 ]]; then
	exit 1
fi

diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
  rm -f output log diff
  exit 0
else
  exit 1
fi
