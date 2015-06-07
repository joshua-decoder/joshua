#!/usr/bin/perl
# Matt Post <post@cs.jhu.edu>

# Takes a list of tab-separated strings on STDIN and prints the lines
# only if none of the fields is empty

use warnings;
use strict;

my $skipped = 0;
while (my $line = <>) {
  if ($line =~ /^\s*\t/ or $line =~ /\t *$/ or $line =~ /\t\s*\t/) {
    $skipped++;
  } else {
    print $line;
  }
}

print STDERR "Skipped $skipped / $.\n";
