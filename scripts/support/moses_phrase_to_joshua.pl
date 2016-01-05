#!/usr/bin/perl

# Converts a Moses phrase table to a Joshua grammar, suitable for packing.
# (Joshua can read in Moses phrase tables directly when using the in-memory
# representation, so in that case there is no need to do the conversion).

use strict;
use warnings;

while (my $line = <>) {
  chomp($line);
  my @tokens = split(/ \|\|\| /, $line);

  unshift(@tokens, "[X]");
  $tokens[3] = join(" ", map { -mylog($_) } split(' ', $tokens[3]));

  print join(" ||| ", @tokens) . $/;
}

sub mylog {
  my ($num) = @_;
  return ($num == 0) ? -100 : log($num);
}
