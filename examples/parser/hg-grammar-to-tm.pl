#!/usr/bin/perl -w
use strict;

while (<>) {
	/(\[[^\]]+,1\])/;
	my $nt1 = $1;
	/(\[[^\]]+,2\])/;
	my $nt2 = $1;
	s/\[X\]/$nt2/;
	s/pt/$nt1/;
	print;
}

