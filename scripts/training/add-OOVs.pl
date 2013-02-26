#!/usr/bin/perl
# Matt Post <2011-02-04>

# Inserts an OOV tag above singleton nodes, using a modified
# definition of "singleton" in which a singleton is defined to be any
# word occuring the minumum number of times any word occurred.  Works
# by taking a stream of Treebank-style parse trees on STDIN, together
# with a lexicon (which contains vocabulary counts).  Writes the
# modified trees to STDOUT.

# Usage:
# cat TREES | add-OOVs.pl LEXICON > TREES.OOV

use strict;
use warnings;

my %SINGLETONS;

my $lexicon_file = shift or die "need lexicon";
# If a second argument is passed, replace the preterminal instead of inserting below it.
my $REPLACE = shift || 0;

# pass over once to find the minimum count of any item in the lexicon
my $min_count = 0;
open LEX, $lexicon_file or die "can't read lexicon '$lexicon_file'";
while (my $line = <LEX>) {
  my ($id,$word,$count) = split(' ',$line);
  $min_count = $count if ($min_count == 0 or $count < $min_count);

  # short-circuit if we find a 1
  last if $min_count == 1;
}
close(LEX);

# pass over again to mark as "singletons" anything appearing min_count times
open LEX, $lexicon_file or die "can't read lexicon '$lexicon_file'";
while (my $line = <LEX>) {
  my ($id,$word,$count) = split(' ',$line);
  $SINGLETONS{$word} = 1 if ($count == $min_count);
}
close(LEX);

while (<>) {
  s/\((\S+)\s+(\S+)\)/maybe_add_OOV($1,$2)/ge;
  print;
}

sub maybe_add_OOV {
  my ($pos,$word) = @_;

  if (exists $SINGLETONS{$word}) {
    return $REPLACE ? "(OOV $word)" : "($pos (OOV $word))";
  } else {
    return "($pos $word)";
  }
}

