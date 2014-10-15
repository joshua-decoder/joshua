#!/bin/sh

cat input | $JOSHUA/bin/joshua-decoder -m 500m -config config 2> log | sort > output

if [[ $? -ne 0 ]]; then
	echo FAILED
	exit 1
fi

diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
  echo PASSED
  rm -f output log diff
  exit 0
else
  echo FAILED
  exit 1
fi
