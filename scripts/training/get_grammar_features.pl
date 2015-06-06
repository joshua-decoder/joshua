#!/usr/bin/env perl

use warnings;
use strict;

# This script retrieves the names of all the features in the grammar. Dense features
# are named with consecutive integers starting at 0, while sparse features can have any name.
# To get the feature names from an unpacked grammar, we have to read through the whole grammar,
# since sparse features can be anywhere. For packed grammars, this can be read directly from
# the encoding.

if (@ARGV != 1) {
  print "Usage: get_grammar_features.pl GRAMMAR\n";
  exit 1;
}

if ((! exists $ENV{JOSHUA}) || (! -d $ENV{JOSHUA})) {
  print "* FATAL: Environment variable \$JOSHUA not set properly\n";
  exit
}

my $CAT = "$ENV{JOSHUA}/scripts/training/scat";

my ($grammar) = @ARGV;

if (-d $grammar) {
  chomp(my @features = `java -d64 -Xmx256m -cp $ENV{JOSHUA}/class joshua.util.encoding.EncoderConfiguration $grammar | grep ^feature: | awk '{print \$NF}'`);
  print join("\n", @features) . $/;

} elsif (-e $grammar) {
  my %features;
  open GRAMMAR, "$CAT $grammar|" or die "FATAL: can't read $grammar";
  while (my $line = <GRAMMAR>) {
    chomp($line);
    my @tokens = split(/ \|\|\| /, $line);
    # field 4 for regular grammars, field 3 for phrase tables
    my $feature_str = ($line =~ /^\[/) ? $tokens[3] : $tokens[2];
    my @features = split(' ', $feature_str);
    my $feature_no = 0;
    foreach my $feature (@features) {
      if ($feature =~ /=/) {
        my ($name) = split(/=/, $feature);
        $features{$name} = 1;
      } else {
        $features{$feature_no++} = 1;
      }
    } 
  }
  close(GRAMMAR);

  print join("\n", keys(%features)) . $/;
}
