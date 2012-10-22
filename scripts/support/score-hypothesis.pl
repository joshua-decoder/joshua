#!/usr/bin/env perl

# Takes (1) a list of feature=value pairs and (2) a weight vector file and produces the score.

use strict;
use warnings;

my %weights;

my $weights_file = shift or die "Usage: score-hypothesis <weights-file>";
open WEIGHTS, $weights_file or die "can't read weights file '$weights_file'";
while (my $line = <WEIGHTS>) {
  chomp($line);
  next if $line =~ /^\s*$/;
  my ($key,$value) = split(' ', $line);

  $weights{$key} = $value;
}
close(WEIGHTS);

my $sum = 0.0;
while (my $line = <>) {
  chomp($line);

  my @pairs = split(' ', $line);
  foreach my $pair (@pairs) {
    my ($key,$value) = split('=', $pair);
    $sum += $weights{$key} * $value if exists $weights{$key};
  }
}

print "$sum\n";
