#!/usr/bin/perl

# Reads any number of file names from the command line, then split()s
# STDIN on tabs and writes them to those files.

use strict;
use warnings;

my @files = @ARGV;
@ARGV = ();

my @fh;
foreach my $file (@files) {
  my $fh;
  if ($file =~ /gz$/) {
	open $fh, "|gzip -9 > $file" or die "can't pipe through gzip";
  } else {
	open $fh, ">", $file or die "can't write to file '$file'";
  }
  push(@fh, $fh);
}

while (my $line = <>) {
  chomp($line);

  my @lines = split(/\t/, $line, scalar @files);

  map {
  	print { $fh[$_] } "$lines[$_]\n";
  } (0..$#fh);
}

map { close($_) } @fh;
