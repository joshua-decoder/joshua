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

my $JOSHUA = $ENV{JOSHUA} or die "you must defined \$JOSHUA";
my $CAT    = "$JOSHUA/scripts/training/scat";

sub usage {
  print "Usage: grammar-packer.pl input-grammar [output-dir=grammar.packed [packer-config]]\n";
  exit;
}

my $grammar = shift or usage();
my $output_dir = shift || "grammar.packed";
my $config  = shift || undef;

system("$CAT $grammar | sort -k3,3 | $JOSHUA/scripts/label_grammar.py | gzip -9n > grammar-labeled.gz");


# Create a dummy packer configuration file, since that is needed by the packer. We do no
# quantization, and simply create a single float quantizer item that applies to all features found
# in the grammar. Note that this isn't recommended for working with sparse grammars!
my $num_features = count_num_features("grammar-labeled.gz");
my $feature_str = join(" ", 0..($num_features-1));
my $packer_config = "packer.config.tmp";
open CONFIG, ">$packer_config" or die "can't write to $packer_config";
print CONFIG "slice_size 400000\n\nquantizer   float   $feature_str\n";
close(CONFIG);

# Do the packing using the config.
system("java -Xmx8g -cp $JOSHUA/class joshua.tools.GrammarPacker -c $packer_config -p $output_dir -g grammar-labeled.gz");

# Clean up.
unlink("grammar-labeled.gz");
system("mv dense_map $output_dir");
system("mv $packer_config $output_dir/packer.config");

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
