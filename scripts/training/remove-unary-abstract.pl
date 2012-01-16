#!/usr/bin/env perl

# Removes unary abstract rules from a grammar file (read from STDIN).
# Unary abstract rules are unary CFG rules that have no terminals
# on the source language side.

LINE: while (my $line = <>) {
  my ($lhs, $source) = split(/ \|\|\| /, $line);

  $total++;

  my @symbols = split(' ', $source);
  # rule passes the filter if (a) it has more than one symbol or (b)
  # it has one symbol and that symbol is not a nonterminal
  if (@symbols > 1 or $symbols[0] !~ /^\[.*,1\]$/) {
	  print $line;
	  next LINE;
  }
  $skipped++;
}

print STDERR "skipped $skipped of $total\n";
