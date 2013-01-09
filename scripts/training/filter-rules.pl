#!/usr/bin/env perl

# Filters grammars according to various criteria.

use strict;
use warnings;
use List::Util qw/max/;
use Getopt::Std;

my %opts;
my $ret = getopts("ups:", \%opts);

if (!$ret) {
  print "Usage: filter-rules.pl [-u] [-s SCOPE]\n";
  print "   -u: remove abstract unary rules\n";
  print "   -p: just print the rule's scope\n";
  print "   -s SCOPE: remove rules with scope > SCOPE (Hopkins & Langmead, 2010)\n";
  exit;
}

my ($total, $skipped) = (0, 0);
 while (my $line = <>) {
  my ($lhs, $source, $target) = split(/ \|\|\| /, $line);

  $total++;

  if ($opts{u}) {
    my @symbols = split(' ', $source);

     # rule passes the filter if (a) it has more than one symbol or (b)
    # it has one symbol and that symbol is not a nonterminal
    if (@symbols == 1 and $symbols[0] =~ /^\[.*,1\]$/) {
      $skipped++;
      next;
    }
  }

  if ($opts{s}) {
    my $scope = get_scope($source);
    if ($opts{p}) {
      chomp($source);
      print "SCOPE($source) = $scope\n";
      next;
    }
    if ($scope > $opts{s}) {
      $skipped++;
      next;
    }
  }

  print $line;
}

print STDERR "skipped $skipped of $total\n";


sub get_scope {
  my ($source) = @_;

  my @tokens = split(' ', $source);

  my $scope = 0;
  for (my $i = 0; $i < @tokens; $i++) {
    my $tok = $tokens[$i];
    if (is_nt($tok) && ($i == 0 || is_nt($tokens[$i-1]))) {
      $scope++;
    }
  }
  $scope++ if (is_nt($tokens[-1]));

  return $scope;
}

sub is_nt {
  my ($word) = @_;

  return 1 if $word =~ /^\[.*,\d+\]$/;
  return 0;
}
