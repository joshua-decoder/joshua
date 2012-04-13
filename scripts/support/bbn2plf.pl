#!/usr/bin/env perl

# Converts BBN FSMs (text format) to Lane Schwartz's PLF format.

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

my @lines;
while (<>) {
  chomp();
  # print "LINE($_)\n";

  # skip comments and blank lines
  next if /^#/ or /^\s*$/;

  # new utterance
  if (/^UTTERANCE/) {
	convert_utterance(@lines) unless $first;
	@lines = ();
	$first = 0;
  }

  push(@lines, $_);
}

convert_utterance(@lines);

sub convert_utterance {
  my @lines = @_;

  for (;;) {
	my $line = shift(@lines);
	my ($label,$id) = split('=', $line);
	die unless ($label eq "UTTERANCE");

	# read in the number of states and edges
	$line = shift(@lines);
	my ($N,$L);
	($label,$N,undef,$L) = split(/ =/, $line);
	die unless ($label eq "N");

	# pass over the nodes, reading what are (I think) priors or state costs
	for (my $n = 0; $n < $N; $n++) {
	  $line = shift(@lines);
	  my ($label,$stateno,undef,$prior) = split(/ =/, $line);
	  die unless $label eq "I";
	  die unless $stateno == $n;
	  $states[$n] = $prior;
	}

	# pass over the edges
	for (my $l = 0; $l < $L; $l++) {
	  $line = shift(@lines);
	  my ($label,$edgeno,undef,$from,undef,$to,undef,$word,@crap) = split(/ =/, $line);
	  die unless $label eq "J" and $edgeno == $l;
	  my $score = pop(@crap);
	  $arcs[$from][$to] = [$word,$score];
	}
  }

  my ($i,$j,undef,$label,$score) = split(' ',$_);

  if (defined $j) {
	if ($j < $i) {
	  print "* FATAL: $j < $i\n";
	  exit;
	}
	push @{$arcs[$i][$j-$i]}, [$label,$score];
  }
}

print "(\n";
foreach my $i (0..$#arcs) {
  print "  (\n";
  foreach my $j (0..$#{$arcs[$i]}) {
	if (defined $arcs[$i][$j]) {
	  foreach my $arc (@{$arcs[$i][$j]}) {
		my ($label,$score) = @$arc;
		$head = $j;
		print "    ('".escape($label)."', $score, $head),\n";
	  }
	}
  }
  print "  ),\n";
}
print ")\n";

sub escape {
  my $arg = shift;
  $arg =~ s/'/\\'/g;
  return $arg;
}
