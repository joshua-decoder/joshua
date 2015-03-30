#!/usr/bin/perl

# Joshua outputs features labels in the form of a list of "key=value"
# pairs, while the Moses scripts expect "key= value" and also require
# that (all and only) sparse feature names contain an underscore. This
# script does the conversion so that we can use Moses' sparse training
# code.

use strict;
use warnings;

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);

  if (@tokens > 1) {

    # Insert an assignment space
    $tokens[2] =~ s/=/= /g;

    # Remove underscores from dense features so they'll not get treated as sparse
    $tokens[2] =~ s/tm_(\w+)_(\d+)=/tm-$1-$2=/g;
    $tokens[2] =~ s/lm_(\d+)=/lm-$1=/g;

    # Add underscores to sparse features so they'll not get treated as dense
    $tokens[2] =~ s/OOVPenalty=/OOV_Penalty=/g;

    print join(" ||| ", @tokens);
  }
}
