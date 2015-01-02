#!/usr/bin/perl

# Joshua outputs features labels in the form of a list of "key=value"
# pairs, while the Moses scripts expect "key= value" and also require
# that sparse feature names contain an underscore. This script does
# the conversion so that we can use Moses' sparse training code.

use strict;
use warnings;

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);

  if (@tokens > 1) {

    # Add an underscore to every feature
    $tokens[2] =~ s/=/_= /g;

    # Remove underscores from dense features so they'll get translated as dense
    $tokens[2] =~ s/tm_(\w+)_(\d+)_=/tm-$1-$2=/g;
    $tokens[2] =~ s/lm_(\d+)_=/lm-$1=/g;
    $tokens[2] =~ s/WordPenalty_=/WordPenalty=/g;
    $tokens[2] =~ s/Distortion_=/Distortion=/g;
    $tokens[2] =~ s/PhrasePenalty_=/PhrasePenalty=/g;

    print join(" ||| ", @tokens);
  }
}
