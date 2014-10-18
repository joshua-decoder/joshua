#!/bin/sh

cat input.txt | $JOSHUA/bin/joshua-decoder -m 500m -c config -maxlen 10 -segment-oovs > output 2> log

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
