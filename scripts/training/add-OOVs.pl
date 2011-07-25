#!/usr/bin/perl
# Matt Post <2011-02-04>

# Inserts an OOV tag above singleton nodes.  Takes a stream of
# Treebank-style parse trees on STDIN, together with a lexicon.
# Writes the modified trees to STDOUT.

# Usage:
# cat TREES | add-OOVs.pl LEXICON > TREES.OOV

use strict;
use warnings;

my %SINGLETONS;

my $lexicon_file = shift or die "need lexicon";
open LEX, $lexicon_file or die "can't read lexicon '$lexicon_file'";
while (my $line = <LEX>) {
  my ($id,$word,$count) = split(' ',$line);
  $SINGLETONS{$word} = 1 if ($count == 1);
}
close(LEX);

while (<>) {
  s/(\S+)\)/maybe_add_OOV($1).")"/ge;
  print;
}

sub maybe_add_OOV {
  my ($word) = @_;

  return (exists $SINGLETONS{$word})
	  ? "(OOV $word)"
	  : $word;
}

