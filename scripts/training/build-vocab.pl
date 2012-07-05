#!/usr/bin/perl

# Takes a corpus of words on STDIN and builds a vocabulary with word
# counts, writing them to STDOUT in the format
#
# ID WORD COUNT

use utf8;
use warnings;
use strict;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");

my %count;
while (my $line = <>) {
  chomp($line);
  my @tokens = split(' ', $line);
  map { $count{$_}++ } @tokens;
}

my $id = 1;
map { print $id++ . " $_ $count{$_}\n" } (sort { $count{$b} <=> $count{$a} } keys %count);
