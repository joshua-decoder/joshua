#!/bin/bash

set -u

# pack the grammar
rm -rf foo.packed bar.packed
$JOSHUA/scripts/support/grammar-packer.pl -v -g 'grammar.gz grammar.gz' -o 'foo.packed bar.packed' 2> packer-multiple.log

diff -q foo.packed/vocabulary bar.packed/vocabulary > diff

if [ $? -eq 0 ]; then
  rm -rf foo.packed bar.packed packer-multiple.log
  exit 0
else
  exit 1
fi
