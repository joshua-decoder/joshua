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

my %opts = (
  m => '4g',    # amount of memory to give the packer
  c => '',       # use alternate packer config
  T => '/state/partition1',
);
getopts("m:c:T:", \%opts);

my $JOSHUA = $ENV{JOSHUA} or die "you must defined \$JOSHUA";
my $CAT    = "$JOSHUA/scripts/training/scat";

sub usage {
  print "Usage: grammar-packer.pl [-m MEM] [-c packer-config] input-grammar [output-dir=grammar.packed\n";
  exit 1;
}

my $grammar = shift or usage();
my $output_dir = shift || "grammar.packed";

my $retval = system("$CAT $grammar | sort -k3,3 --buffer-size=$opts{m} -T $opts{T} | $JOSHUA/scripts/label_grammar.py | gzip -9n > grammar-labeled.gz");
if ($retval != 0) {
  print STDERR "* FATAL: sorting and labeling failed\n";
  exit 1;
}

# Create a dummy packer configuration file, since that is needed by the packer. We do no
# quantization, and simply create a single float quantizer item that applies to all features found
# in the grammar. Note that this isn't recommended for working with sparse grammars!
my $num_features = count_num_features("grammar-labeled.gz");
my $feature_str = join(" ", 0..($num_features-1));
my $packer_config = "packer.config.tmp";
open CONFIG, ">$packer_config" or die "can't write to $packer_config";
print CONFIG "slice_size 100000\n\nquantizer   float   $feature_str\n";
close(CONFIG);

# Do the packing using the config.
my $cmd = "java -Xmx$opts{m} -cp $JOSHUA/class joshua.tools.GrammarPacker -c $packer_config -p $output_dir -g grammar-labeled.gz";
print STDERR "Packing with $cmd\n";
my $retval = system($cmd);

if ($retval == 0) {
  # Clean up.
  unlink("grammar-labeled.gz");
  system("mv dense_map $output_dir");
  system("mv $packer_config $output_dir/packer.config");
} else {
  print STDERR "* FATAL: Couldn't pack the grammar.\n";
  exit 1;
}

################################################################################
## SUBROUTINES #################################################################
################################################################################

# This counts the number of TM features present in a grammar
sub count_num_features {
  my ($grammar) = @_;

  open GRAMMAR, "$CAT $grammar|" or die "FATAL: can't read $grammar";
  chomp(my $line = <GRAMMAR>);
  close(GRAMMAR);

  my @tokens = split(/ \|\|\| /, $line);
  my @numfeatures = split(' ', $tokens[-1]);
	my $num = scalar(@numfeatures);

  return scalar @numfeatures;
}
