#!/usr/bin/perl

# Joshua outputs features labels in the form of a list of "key=value" pairs. In contrast, the Moses
# scripts expect feature labels to end with a colon and to have a space between them and their
# value(s) (there can be many). This script converts to the Moses format, and also appends an
# underscore to all feature labels, which the Moses scripts use to mark sparse features.

use strict;
use warnings;

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);
  $tokens[2] =~ s/=/_: /g;
  print join(" ||| ", @tokens);
}
