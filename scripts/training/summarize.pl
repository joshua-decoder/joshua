#!/usr/bin/perl

use strict;
use warnings;

opendir DIR, "." or die;
my @dirs = sort { $a <=> $b } grep /^\d[\d\.]*$/, readdir DIR;
closedir DIR;

foreach my $dir (@dirs) {
  chomp(my $readme = `cat $dir/README`);
  my $bleu = get_bleu("$dir/test/final-bleu");
  # my $mbr =  get_bleu("$dir/test/final-bleu-mbr");

  my $dirstring = dirstring($dir);

  # print "$dirstring\t$bleu\t$mbr\t$readme\n";
  print "$dirstring\t$bleu\t$readme\n";
}

sub get_bleu {
  my ($file) = @_;

  my $score = 0.0;
  my $num_scores = 0;
  if (-e $file) {
    chomp($score = `cat $file`);
    my @tokens = split(' ', $score);
    $num_scores = 1;
    foreach my $token (@tokens) {
      $num_scores++ if $token eq "+";
    }

    $score = $tokens[-1] * 100;
  }

  return sprintf("%5.2f", $score) . "($num_scores)";
}

sub dirstring {
  my ($dir) = @_;

  my $num_periods = 0;
  $num_periods++ while ($dir =~ /\./g);

  my $dirname = "> " x $num_periods;
  $dirname .= $dir;
  return $dirname;
}
