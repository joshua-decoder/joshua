#!/usr/bin/env perl
# This script converts an N-best list from Joshua into a 1-best list
#
# Original by Zhifei Li
# Modified by wren ng thornton
#     (forked from Zhifei's   on Wed Aug 27 21:38:24 EDT 2008)
#     (forked from darcs repo on Fri Oct 17 21:34:17 EDT 2008)

use warnings;
use strict;

my ($file_nbest, $file_1best) = @ARGV;
die "Usage: $0 nbest_inut 1best_outut\n(or use - for stdin/stdout)\n"
	unless $file_nbest and $file_1best and 2 == @ARGV;

open *STDIN,  '<', $file_nbest
	or die "Couldn't open $file_nbest: $!\n"
	unless $file_nbest eq '-';

open *STDOUT, '>', $file_1best
	or die "Couldn't open $file_1best: $!\n"
	unless $file_nbest eq '-';


my $old_id = -1;
while (my $line = <STDIN>) { chomp $line;
	
	# Fields are seperated by " ||| "
	my ($new_id, $translation) = split /\s+\|{3}\s+/, $line;
	die "Malformed file"
		unless defined $new_id and $translation;
	
	# Always take the first line of a group
	if ($old_id == -1 or $new_id != $old_id) {
		print $translation, "\n";
		$old_id = $new_id;
	}
}