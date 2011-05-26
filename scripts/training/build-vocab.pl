#!/usr/bin/perl

# Takes a corpus of words on STDIN and builds a vocabulary with word
# counts, writing them to STDOUT in the format
#
# ID WORD COUNT

while (<>) {
  chomp;
  split;
  map { $count{$_}++ } @_;
}

$id = 1;
map { print $id++ . " $_ $count{$_}\n" } (keys %count);
