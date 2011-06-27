#!/usr/bin/perl
# Matt Post <post@jhu.edu> March 2011

# takes two parallel files as arguments, plus a third arg (the
# threshold).  Outputs sentence pairs where both sides meet that
# threshold.  file1 is output to STDOUT, file2 to STDERR.

# e.g.,
# paste corpus.en.corpus.fr | trim_parallel_corpus.pl 40 > corpus.en.40 2> corpus.fr.40

my $thresh = shift || 100;

while (<>) {
  my ($line1,$line2) = split(/\t/,$_,2);

  last unless $line1 and $line2;

  my @tokens1 = split(' ', $line1);
  my @tokens2 = split(' ', $line2);

  next if (@tokens1 > $thresh || @tokens2 > $thresh) || @tokens1 == 0 || @tokens2 == 0;

  print STDOUT $line1;
  print STDERR $line2;
}
