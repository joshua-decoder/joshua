#!/usr/bin/perl

use strict;
use warnings;

my $basedir = shift(@ARGV) || ".";

opendir DIR, $basedir or die;
my @dirs = sort { $a <=> $b } grep /^\d[\d\.]*$/, readdir DIR;
closedir DIR;

foreach my $dir (@dirs) {
  chomp(my $readme = `[[ -e "$basedir/$dir/README" ]] && cat $basedir/$dir/README`);
  my $bleu = get_scores("$basedir/$dir/test/final-bleu", 100.0);
  my $meteor = get_scores("$basedir/$dir/test/final-meteor", 100.0);
  my $time = get_time("$basedir/$dir/test/final-times");
  # my $mbr =  get_bleu("$dir/test/final-bleu-mbr");

  my $dirstring = dirstring($dir);

  # print "$dirstring\t$bleu\t$mbr\t$readme\n";
  print "$dirstring\t$bleu\t$meteor\t$time\t$readme\n";
}

sub get_scores {
  my ($file, $factor) = @_;
  $factor = 1.0 unless $factor;

  my $score = 0.0;
  my $num_scores = 0;
  if (-e $file) {
    chomp($score = `cat $file`);
    my @tokens = split(' ', $score);
    $num_scores = 1;
    foreach my $token (@tokens) {
      $num_scores++ if $token eq "+";
    }

    $score = $tokens[-1] * $factor;
  }

  $score /= $factor if $score > $factor;

  return sprintf("%5.2f", $score) . "($num_scores)";
}

sub get_time {
  my ($file) = @_;

  my $seconds = 0.0;
  if (-e $file) {
    chomp($seconds = `cat $file`);
    my @tokens = split(' ', $seconds);
    $seconds = $tokens[-1];
  }

  my $hours = int($seconds / 3600);
  $seconds %= 3600;
  my $minutes = int($seconds / 60);
  $seconds %= 60;

  return "$hours:$minutes:$seconds";
}

sub dirstring {
  my ($dir) = @_;

  my $num_periods = 0;
  $num_periods++ while ($dir =~ /\./g);

  my $dirname = "> " x $num_periods;
  $dirname .= $dir;
  return $dirname;
}
