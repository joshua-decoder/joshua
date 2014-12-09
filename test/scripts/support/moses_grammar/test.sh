#!/bin/bash


cat input | $JOSHUA/scripts/support/moses2joshua_grammar.pl > output

diff -u output output.expected > diff

if [ $? -eq 0 ]; then
  rm -f diff output
  exit 0
else
  exit 1
fi


