#!/usr/bin/perl -w
use strict;

my $limit = shift @ARGV;

while (<>) {
	my @tokens = split;
	print unless scalar(@tokens) > $limit;
}

