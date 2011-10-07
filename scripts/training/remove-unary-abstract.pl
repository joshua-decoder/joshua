#!/usr/bin/perl

# Removes unary abstract rules from a grammar file (read from STDIN).
# Unary abstract rules are unary CFG rules that have no terminals
# on the source language side.

LINE: while (my $line = <>) {
	my ($lhs, $source) = split(/ \|\|\| /, $line);

	$total++;

        my @symbols = split(' ', $source);
        foreach my $symbol  (@symbols) {
		if ($symbol !~ /^\[.*\]$/ or $symbol =~ /,2/) {
			print $line;
			next LINE;
		}
	}
	$skipped++;
#	print STDERR "SKIPPING $line";
}

print STDERR "skipped $skipped of $total\n";
