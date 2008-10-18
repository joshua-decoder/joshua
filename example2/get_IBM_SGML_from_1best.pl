#!/usr/bin/env perl
# This program converts a 1-best translation file and a collection
# of reference files which are all sentence/line aligned into
# sufficient SGML to run IBM's BLEU script. That is, we make up the
# document and segment ids and anything else we need to.
#
# Note that, since BLEU is document based, this is a bad idea if you'll be grouping many documents together thereby.

use warnings;
use strict;

my ($stem) = @ARGV;
die "Usage: $0 file_stem\n" .
	"\nWe assume that files are named stem.1best, stem.ref.0, stem.ref.1,...\n"
	unless $stem and 1 == @ARGV;

sub safe_open { my ($mode, $filename) = @_;
	open my $fh, $mode, $filename
		or die "Couldn't open file $filename: $!\n";
	return $fh;
}



*STDIN  = safe_open('<', "$stem.1best");
*STDOUT = safe_open('>', "$stem.1best.sgm");

print "<srcset setid=\"$stem\">\n",
	"<doc docid=\"$stem\" sysid=\"Joshua\">\n";
my $segid = 0;
while (my $line = <STDIN>) { chomp $line;
	$segid++;
	print "<seg id=\"$segid\">$line</seg>\n";
}
print "</doc>\n",
	"</srcset>\n";



*STDOUT = safe_open('>', "$stem.refs.sgm");

print "<refset setid=\"$stem\">\n";
my $refid = 0;
while (-e "$stem.ref.$refid") {
	*STDIN = safe_open('<', "$stem.ref.$refid");
	$refid++;
	
	print "<doc docid=\"$stem\" sysid=\"$refid\">\n";
	$segid = 0;
	while (my $line = <STDIN>) { chomp $line;
		$segid++;
		print "<seg id=\"$segid\">$line</seg>\n";
	}
	print "</doc>\n";
};
print "</refset>\n";

__END__
