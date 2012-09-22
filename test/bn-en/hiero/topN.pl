#!/usr/bin/perl

my $N = shift || 10;
my $field = -1;
my $count = 0;
while (<>) {
  my @tokens = split;
  if ($tokens[0] != $field) {
	$count = 0;
	print;
	$field = $tokens[0];
  } elsif ($count >= $N) {
	next;
  } else {
        print;
  }
  $count++;
}
