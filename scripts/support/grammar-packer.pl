#!/usr/bin/env perl

use strict;
use warnings;

my $JOSHUA = $ENV{JOSHUA} or die "you must defined \$JOSHUA";
my $CAT    = "$JOSHUA/scripts/training/scat";

my $grammar = shift;
my $output_dir = shift || "grammar.packed";
my $config  = shift || undef;

system("$CAT $grammar | sort -k3,3 | $JOSHUA/scripts/label_grammar.py | gzip -9n > grammar-labeled.gz");

my $num_features = count_num_features("grammar-labeled.gz");

my $feature_str = join(" ", 0..($num_features-1));

die "packer.config already exists, refusing to overwrite" if -e "packer.config";
open CONFIG, ">packer.config" or die "can't write to packer.config";
print CONFIG "slice_size 400000\n\nquantizer   float   $feature_str\n";
close(CONFIG);

system("java -cp $JOSHUA/bin joshua.tools.GrammarPacker -c packer.config -p $output_dir -g grammar-labeled.gz");

unlink("grammar-labeled.gz");
system("mv dense_map packer.config $output_dir");


## SUBROUTINES #################################################################

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
