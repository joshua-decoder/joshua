#!/usr/bin/env perl

# Converts BBN FSMs (text format) to Lane Schwartz's PLF format.
# Usage: cat BBN-FILE | bbn2plf.pl > PLF-FILE

# FORMAT:
# - optional comments (#) and blank lines
# UTTERANCE=ID
# N=[num states] L=[num edges]
# I=0 t=[double]
# ...
# I=N-1 ...
# J=0 s=X E=X W=[word] v=? a=? l=? s=[double]
# ...
# J=L-1 ...
#
# where the Is list states and the Js enumerate edges

use strict;

my @lines;
my @states;
my $head;
{

# read in one utterance at a time
local $/ = 'UTTERANCE';
while (<>) {
  chomp();
  my $utterance = $_;
  # skip comments and blank lines
  $utterance =~ s/#.*//gm;
  $utterance =~ s/^\s*$//gm;
  # print "LINE($_)\n";
  # print STDERR "utterance: '$utterance'\n";
  unless($utterance =~ /^=/){next}
  @lines = split /\n/, $utterance;
  convert_utterance(@lines);
}

}

# this function prepends backslashes in front of single-quotes
sub escape {
  my $arg = shift;
  $arg =~ s/'/\\'/g;
  return $arg;
}

sub convert_utterance {
  my @lines = @_;
	my @arcs;

	# loop until we've read everything
  while(@lines > 0){
		# the first line better be an utterance marker (sanity check)
		# which, in this case, means it starts with '=' (we stripped
		# off the 'UTTERANCE' while reading in)
		my $numlines = @lines;
		my $line = shift(@lines);
		#clean up any lingering comments or blank lines
		while($line =~ /^\s*$/ or $line =~ /^#/){$line = shift(@lines)}
		#clean up any remaining 'UTTERANCE's
		chomp $line;
		die "Failed sanity check: first line ('$line') is not an utterance\n" unless $line =~ /^=/;

		my (undef, $id) = split('=', $line);

		# read in the number of states and edges
		$line = shift(@lines);
		my ($label,$N,undef,$L) = split(/[ =]/, $line);
		die "Problem reading states and edges: '$label' is not 'N' in '$line'\n" unless ($label eq "N");

		# pass over the nodes, reading what are (I think) priors or state costs
		for (my $n = 0; $n < $N; $n++) {
			$line = shift(@lines);
			while($line =~ /^\s*$/ or $line =~ /^#/) {$line = shift(@lines)}
			my ($label,$stateno,undef,$prior) = split(/[ =]/, $line);
			die "Problem reading node '$line': '$label' != 'I'\n" unless $label eq "I";
			die "Problem reading node '$line': '$stateno' != '$n'\n" unless $stateno == $n;
			$states[$n] = $prior;
		}

		# pass over the edges.  arcs is a two-level table marking (from,to) pairs
		for (my $l = 0; $l < $L; $l++) {
			$line = shift(@lines);
			while($line =~ /^\s*$/ or $line =~ /^#/) {$line = shift(@lines)}
			my ($label,$edgeno,undef,$from,undef,$to,undef,$word,@crap) = split(/[ =]/, $line);
			die "Problem reading edge '$line': '$label' != 'J' or '$edgeno' != '$l'\n" unless $label eq "J" and $edgeno == $l;
			my $score = pop(@crap);
			my @pair = ($word, $score);
			if( $arcs[$from][$to]){
				push @{$arcs[$from][$to]}, \@pair;
			}
			else{
				my @pairslist = (\@pair);
				$arcs[$from][$to] = \@pairslist;
			}
		}
  }

#turn the 'to's into offsets
my @newarcs;
for(my $i=0; $i<@arcs; $i++){
	for(my $j=$i; $j<@{$arcs[$i]}; $j++){
		if (defined $arcs[$i][$j]){
			foreach my $pair (@{$arcs[$i][$j]}){
				my $newj = $j-$i;
				push @{$newarcs[$i][$newj]}, $pair;
			}
		}
	}
}
@arcs = @newarcs;

  # now print out the lattices
	print "(\n";
	foreach my $i (0..@arcs) {
		if (defined $arcs[$i]){
		    print " (\n";
		    foreach my $j (0..@{$arcs[$i]}) {
			if (defined $arcs[$i][$j]) {
				foreach my $arc (@{$arcs[$i][$j]}) {
					my ($label,$score) = @$arc;
					$head = $j;
					print "  ('".escape($label)."', $head, $score),";
				}
			}
		    }
		    print "\n ),\n";
		}
	}
	print ")\n";
}
