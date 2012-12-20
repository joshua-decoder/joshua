#!/usr/bin/perl

# Reads a stream from STDIN, splits them on tabs, and writes the fields to each of a series of
# filenames, respectively, passed as arguments to the script.  If a filename ends in .gz, output
# will be compressed using gzip.

use strict;
use warnings;
use FileHandle;

$| = 1;   # don't buffer output

if (@ARGV <= 0) {
  print "Usage: cat tabbed-file | splittabs.pl file1 [file2 [file3 ...]]\n";
  exit;
}

my @fh = map { get_filehandle($_) } @ARGV;
@ARGV = ();

while (my $line = <>) {
  chomp($line);
  my (@fields) = split(/\t/,$line,scalar @fh);
  
  map { print {$fh[$_]} "$fields[$_]\n" } (0..$#fields);
}

sub get_filehandle {
  my $file = shift;

  if ($file eq "-") {
    return *STDOUT;
  } elsif ($file =~ /.gz$/) {
    local *FH;
    open FH, "| gzip -9n > $file" or die "can't open compressed file '$file' for writing";
    return *FH;
  } else {
    local *FH;
    open FH, ">$file" or die "can't open file '$file' for writing";
    return *FH;
  }
}
