#!/usr/bin/env perl

# This script packs a grammar dense, unlabeled grammar, where we don't care what labels we give to
# the feature functions.
#
# Usage:
#
#    grammar-packer.pl input-grammar [output-dir [packer-config]]
#
# where
#
#    input-grammar is the input grammar to be packed
#    output-dir is the packed grammar directory to write to (default: grammar.packed)
#    packer-config is the packer config file (default: all floats)

use strict;
use warnings;
use Getopt::Std;
use File::Temp qw/tempfile/;
use File::Basename qw/basename/;

my %opts = (
  m => '8g',      # amount of memory to give the packer
  T => '/tmp',    # location of temporary space
  v => 0,         # verbose
);
getopts("m:T:v", \%opts);

my $JOSHUA = $ENV{JOSHUA} or die "you must defined \$JOSHUA";
my $CAT    = "$JOSHUA/scripts/training/scat";

sub usage {
  print "Usage: grammar-packer.pl [-m MEM] [-T /path/to/tmp] input-grammar [output-dir=grammar.packed]\n";
  exit 1;
}

my $grammar = shift or usage();
my $output_dir = shift || "grammar.packed";

if (! -e $grammar) {
  print "* FATAL: Can't find grammar '$grammar'\n";
  exit 1;
}

# Sort the grammar or phrase table
my $name = basename($grammar);
my (undef,$sorted_grammar) = tempfile("${name}XXXX", DIR => $opts{T}, UNLINK => 1);
print STDERR "Sorting grammar to $sorted_grammar...\n" if $opts{v};

# We need to sort by source side, which is field 0 (for phrase tables not listing the LHS)
# or field 1 (convention, Thrax format)
chomp(my $first_line = `$CAT $grammar | head -n1`);
if ($first_line =~ /^\[/) {
  # regular grammar
  if (system("$CAT $grammar | sort -k3,3 --buffer-size=$opts{m} -T $opts{T} | gzip -9n > $sorted_grammar")) {
    print STDERR "* FATAL: Couldn't sort the grammar (not enough memory? short on tmp space?)\n";
    exit 2;
  }
} else {
  # Moses phrase-based grammar -- prepend nonterminal symbol and -log() the weights
  if (system("$CAT $grammar | $JOSHUA/scripts/support/moses_phrase_to_joshua.pl | sort -k3,3 --buffer-size=$opts{m} -T $opts{T} | gzip -9n > $sorted_grammar")) {
    print STDERR "* FATAL: Couldn't sort the grammar (not enough memory? short on tmp space?)\n";
    exit 2;
  }
}  
#my $source_field = ($first_line =~ /^\[/) ? "3,3" : "1,1";

$grammar = $sorted_grammar;

# Do the packing using the config.
my $cmd = "java -Xmx$opts{m} -cp $JOSHUA/class joshua.tools.GrammarPacker -p $output_dir -g $grammar";
print STDERR "Packing with $cmd...\n" if $opts{v};
my $retval = system($cmd);

if ($retval == 0) {
  unlink($sorted_grammar);
} else {
  print STDERR "* FATAL: Couldn't pack the grammar.\n";
  exit 1;
}
