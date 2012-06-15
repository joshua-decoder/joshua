#!/usr/bin/perl

# Takes a corpus of words on STDIN and builds a vocabulary with word
# counts, writing them to STDOUT in the format
#
# ID WORD COUNT

use utf8;

binmode(STDIN,  ":utf-8");
binmode(STDOUT, ":utf-8");

while (<>) {
  chomp;
  split;
  map { $count{$_}++ } @_;
}

$id = 1;
map { print $id++ . " $_ $count{$_}\n" } (sort { $b <=> $a } keys %count);
