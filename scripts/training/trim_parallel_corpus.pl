#!/usr/bin/perl
# Matt Post <post@jhu.edu> March 2011

# takes two parallel files as arguments, plus a third arg (the
# threshold).  Outputs sentence pairs where both sides meet that
# threshold.  file1 is output to STDOUT, file2 to STDERR.

# e.g.,
# trim_parallel_corpus.pl corpus.en corpus.fr 40 > corpus.en.40 2> corpus.fr.40

my $lang1_file = shift;
my $lang2_file = shift;
my $thresh = shift || 100;

open READ1, $lang1_file or die;
open READ2, $lang2_file or die;

for (;;) {
  my $line1 = <READ1>;
  my $line2 = <READ2>;

  last unless $line1 and $line2;

  my @tokens1 = split(' ', $line1);
  my @tokens2 = split(' ', $line2);

  next if (@tokens1 > $thresh || @tokens2 > $thresh) || @tokens1 == 0 || @tokens2 == 0;

  print STDOUT $line1;
  print STDERR $line2;
}
