#!/usr/bin/env perl
# Matt Post <post@cs.jhu.edu>

# Converts a Moses grammar to a Joshua grammar.
#
# Usage: cat grammar.moses | moses2joshua_grammar.pl > grammar.joshua
#
# The notable differences between the formats are as follows:
#
# (1) The rule syntax. Moses' rules look like this:
# 
#     der [X][NN] [X] ||| of the [X][NN] [PP] ||| 0-0 0-1 1-2 ||| 1 ||| |||
#
# Whereas the corresponding Joshua rule looks like this:
# 
#     [PP] ||| der [NN] ||| of the [NN] ||| 1
#
# (2) Phrase table values. Joshua negates the phrase table feature values upon reading them in,
# whereas Moses (more sensibly) does not.

use strict;
use warnings;
use File::Basename;
use Getopt::Std;

my %opts;
getopts("m:",\%opts);

sub usage {
  print "Usage: cat moses.grammar | moses2joshua_grammar.pl [-m TREE_MAP_FILE] > joshua.grammar\n";
  print "where TREE_MAP_FILE maps rule target-sides to internal trees\n";

  exit;
}

  # Rules look like the following, with many of the fields optional
  #
  # <s> [X] ||| <s> [S] ||| [weights] ||| [alignments] ||| [counts] ||| [tree]
  # [X][S] </s> [X] ||| [X][S] </s> [S] ||| 1 ||| 0-0 ||| 0
  # [X][S] [X][X] [X] ||| [X][S] [X][X] [S] ||| 2.718 ||| 0-0 1-1 ||| 0

if ($opts{m}) {
  open MAP, ">$opts{m}" or die "can't write map to file '$opts{m}'";
}

while (my $rule = <>) {
  chomp($rule);

  my $orig_rule = $rule;

# ! es [X][VP] , [X] ||| , we [X][VP] , [PRN] ||| 0.0102041 0.00950695 1 0.000537615 2.718 ||| 2-2 ||| 98 1
# ! es [X][VP] [X][,] [X] ||| , we [X][VP] [X][,] [PRN] ||| 0.0120482 0.0206611 1 0.000867691 2.718 ||| 2-2 3-3 ||| 83 1

  # Joshua doesn't support tree-to-string (currently), so we get rid of the source-side
  # nonterminal. This also simplifies later processing.
  $rule =~ s/ \[\S+?\](\[\S+?\])/ $1/g;

  my ($l1, $l2, $probs, $alignment, $counts, undef, $tree) = split(/\s*\|\|\|\s*/, $rule);

  # The source-side nonterminals (of each pair) have been removed. Here we push all the for the
  # source and target sides into arrays. We grab the LHS from the target side list.
  my (@l1tokens) = split(' ', $l1);
  pop(@l1tokens);
  my (@l2tokens) = split(' ', $l2);
  my $lhs = pop(@l2tokens);

  # Now build a list of just the nonterminals. Then for the target side (L2), we map positions in
  # the string to its index in the list of nonterminals, used to discover the permutation later on.
  my (@l1nts);
  for (my $i = 0; $i < @l1tokens; $i++) {
    my $token = $l1tokens[$i];
    if ($token =~ /\[(\S+?)\]/g) {
      push(@l1nts, $1);
    }
  }
  my (@l2nts,@l2orders);
  my $num_lhs_seen = 0;
  for (my $i = 0; $i < @l2tokens; $i++) {
    my $token = $l2tokens[$i];
    if ($token =~ /\[(\S+?)\]/g) {
      push(@l2nts, $1);
      $l2orders[$i] = ++$num_lhs_seen;
    }
  }

  if (scalar @l1nts != scalar @l2nts) {
    print STDERR "* WARNING: nonterminal count mismatch on RHS\n";
    print STDERR "*  " . (scalar @l1nts) . $/;
    print STDERR "*  " . (scalar @l2nts) . $/;
    next;
  }

  # Build the permutation by first sorting L1 and then using the order index.
  my @permutation;
  my @alignments = sort by_first split(' ', $alignment);
  foreach my $pair (@alignments) {
    my ($l1,$l2) = split(/-/, $pair);
    push(@permutation, $l2orders[$l2]) if defined $l2orders[$l2];
  }

  if (scalar(@permutation) != scalar(@l2nts)) {
    my $a = scalar(@l2nts);
    my $b = scalar(@permutation);
    print STDERR "* [line $.] WARNING: permutation length is too short (l2nts $a, perm $b)\n";
    next;
  }

  # Now go on and print the rule.
  my $new_rule = "$lhs |||";
  $num_lhs_seen = 0;
  foreach my $token (@l1tokens) {
    if ($token =~ /\[(\S+?)\]/g) {
      $new_rule .= " [$1," . (++$num_lhs_seen) . "]";
    } else {
      $new_rule .= " $token";
    }
  }

  $new_rule .= " |||";

  $num_lhs_seen = 0;
  my $target = "";
  foreach my $token (@l2tokens) {
    if ($token =~ /\[(\S+?)\]/g) {
      $target .= " [$1," . $permutation[$num_lhs_seen++] . "]";
    } else {
      $target .= " $token";
    }
  }
  $target =~ s/^\s+//;
  $new_rule .= " " . $target;

  my @probs = map { transform($_) } (split(' ',$probs));
  # Moses no longer uses exp(1) as its phrase penalty feature, so we can't
  # rely on the uniform transform above...
  $probs[-1] = 1.0;
  my $scores = join(" ", map { sprintf("%.5f", $_) } @probs);

#  $new_rule .= " ||| $scores ||| $alignment ||| $counts";
  $new_rule .= " ||| $scores";

  # print STDERR "  NEW_RULE: $new_rule$/";
  print "$new_rule\n";

  if ($opts{m} and defined $tree) {
    $tree =~ s/.*{{Tree\s+(.*)}}.*/$1/;
    # Remove brackets around substitution points
    $tree =~ s/\[([^\[\]\s]+)\]/$1/g;
    # Add quotes around terminals
    $tree =~ s/\[([^\[\]]+) ([^\[\]]+)\]/[$1 "$2"]/g;
    $tree =~ s/\[/(/g;
    $tree =~ s/\]/)/g;

    print MAP "$tree ||| $target\n";
  }
}

close(MAP) if ($opts{m});

sub transform {
  my ($weight) = @_;

  # Moses defines the log_e() of non-positive weights as -100
  # Return -1 times this, since Joshua negates all feature weights from the grammar
  return "100" if ($weight <= 0.0);
  
  # if ($weight eq "2.718") {
  # 	return $weight;
  # } else {
  # 	return -log($weight);
  # }

  return -log($weight);
}

# Reads the moses config file to look for the span limit for grammar i (0-indexed).
sub get_span_limit {
  my ($config_file, $index) = @_;

  open READ, $config_file or die "can't find config file '$config_file'";
  while (my $line = <READ>) {
    if ($line =~ /max-chart-span/) {
      # Burn through a number of lines until we get to the one we need.  This assumes there are no
      # intervening comments or blank lines.
      for (my $i = 0; $i < $index; $i++) {
        my $t = <READ>;
      }
      last;
    }
  }
  chomp(my $max_span = <READ>);

  close(READ);
  return $max_span;
}

sub by_first {
  my $a1 = $a;
  $a1 =~ s/-.*//;
  my $b1 = $b;
  $b1 =~ s/-.*//;
  return $a1 <=> $b1;
}

