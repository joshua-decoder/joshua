#!/usr/bin/perl
# Matt Post <post@cs.jhu.edu>

# Takes a list of tab-separated strings on STDIN and a single argument N, a threshold. If either of
# the first two fields has mroe than N tokens, the line is skipped.

# e.g.,
# paste corpus.en corpus.fr | trim_parallel_corpus.pl 40 | split2files.pl en.trimmed.40 fr.trimmed.40

my $thresh = shift || 100;

while (my $line = <>) {
  my ($line1,$line2,$rest) = split(/\t/,$line,3);

  # Make sure they're both defined
  next unless (defined $line1 and defined $line2);

  # Skip if either side is over the threshold
  my @tokens1 = split(' ', $line1);
  my @tokens2 = split(' ', $line2);
  next if (@tokens1 > $thresh || @tokens2 > $thresh) || @tokens1 == 0 || @tokens2 == 0;

  # Otherwise print the whole line
  print $line;
}
