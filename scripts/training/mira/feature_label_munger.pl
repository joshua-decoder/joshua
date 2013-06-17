#!/usr/bin/perl

# Joshua outputs features labels in the form of a list of "key=value"
# pairs, while the Moses scripts expect "key= value" and also require
# that sparse feature names contain an underscore. This script does
# the conversion so that we can use Moses' sparse training code.

use strict;
use warnings;

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);
  $tokens[2] =~ s/=/_= /g;
  print join(" ||| ", @tokens);
}
