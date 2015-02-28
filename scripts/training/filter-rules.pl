#!/usr/bin/env perl

# Filters grammars according to various criteria.

use strict;
use warnings;
use List::Util qw/max/;
use Getopt::Std;

my %opts;
my $ret = getopts("bps:uv", \%opts);

if (!$ret) {
  print "Usage: filter-rules.pl [-u] [-s SCOPE] [-v]\n";
  print "   -b: skip blank source and target sides\n";
  print "   -p: just print the rule's scope\n";
  print "   -s SCOPE: remove rules with scope > SCOPE (Hopkins & Langmead, 2010)\n";
  print "   -u: remove abstract unary rules\n";
  print "   -v: be verbose\n";
  exit;
}

my ($total, $skipped) = (0, 0);
my %skipped = (
  unary => 0,
  lex_scope => 0,
  unlex_scope => 0
);
while (my $line = <>) {
  my ($source, $target);
  if ($line =~ /^\[/) {
    # hierarchical grammars
    (undef, $source, $target) = split(/ \|\|\| /, $line);
  } else {
    # phrase table
    ($source, $target) = split(/ \|\|\| /, $line);
  }
  $total++;

  if ($opts{b}) {
    if ($source =~ /^\s*$/ or $target =~ /^\s*$/) {
      $skipped{blanks}++;
      $skipped++;
      next;
    }
  }

  if ($opts{u}) {
    my @symbols = split(' ', $source);

     # rule passes the filter if (a) it has more than one symbol or (b)
    # it has one symbol and that symbol is not a nonterminal
    if (@symbols == 1 and $symbols[0] =~ /^\[.*,1\]$/) {
      print STDERR "SKIPPING unary abstract rule $line" if $opts{v};
      $skipped{unary}++;
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
      print STDERR "SKIPPING out-of-scope rule $line" if $opts{v};
      $skipped{scope}++;
      $skipped++;

      if (is_lex($source)) {
        $skipped{"lex"}++;
      } else {
        $skipped{"unlex"}++;
      }
      next;
    }
  }

  print $line;
}

print STDERR "filter-rules.pl: skipped $skipped of $total rules\n";
foreach my $key (keys %skipped) {
  print STDERR "  skipped $key: $skipped{$key}\n";
}

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

sub is_lex {
  my ($side) = @_;
  return grep { ! is_nt($_) } split(' ', $side);
}
