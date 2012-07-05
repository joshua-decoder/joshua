#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum

rm -f test.nbest test.1best

cat test.plf | $JOSHUA/joshua-decoder -m 500m config.test > test.nbest 2> log

if [[ $? -ne 0 ]]; then
	echo FAILED
	exit 1
fi

diff -u test.nbest test.expected > diff

if [[ $? -eq 0 ]]; then
  echo PASSED
  exit 0
else
  echo FAILED
  cat diff
	exit $?
fi


