#!/usr/bin/perl -w
use strict;

my %parsed;
my %failed;

while (<>) {
	if (/GOAL-(\d+)/) {
		# parsed successfully
		$parsed{$1}++;
	}
	else {
		# failed
		my @parts = split /\|/;
		my $english = $parts[6];
		$english =~ s/^ //;
		my @words = split /\s+/, $english;
		print if (scalar @words == 11);
		$failed{scalar @words}++;
	}
}

print "length\tparsed\tfailed\n";
for (my $i = 1; $i <= 50; $i++) {
	my $parsecount = defined $parsed{$i} ? $parsed{$i} : 0;
	my $failcount = defined $failed{$i} ? $failed{$i} : 0;
	print "$i\t$parsecount\t$failcount\n";
}
