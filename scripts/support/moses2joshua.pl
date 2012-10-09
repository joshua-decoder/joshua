#!/usr/bin/perl
# 2011-07-14 Matt Post <post@jhu.edu>

# Converts a Moses configuration file to a Joshua configuration file
# (including phrase table conversion)
#
# Usage: moses2joshua.pl moses.ini
#
# This command will produce (1) joshua.config and (2) a phrase table
# named as in the moses file.

use strict;
use warnings;
use File::Basename;
use Getopt::Std;

my %opts;
getopts("gt",\%opts);

my ($moses_ini_file, $outdir) = @ARGV;
if (! defined $moses_ini_file) {
  print "Usage: moses2joshua.pl [-g] moses.ini [outputdir]\n";
  print "where\n  -t      translate regular grammar\n";
  print "where\n  -g      translate glue grammar\n";
  exit;
}

$outdir = "joshua" unless defined $outdir;

print STDERR "using outdir '$outdir', moses file = $moses_ini_file\n";

my (@grammars);
my $NUMFEATURES = 0;

open MOSES, $moses_ini_file or die "can't find moses file '$moses_ini_file'";

system("mkdir","-p",$outdir) unless -d $outdir;
open JOSHUA, ">", "$outdir/joshua.config" or die;
select(JOSHUA);

while (my $line = <MOSES>) {
  chomp($line);
  next if $line =~ /^#/;

#  print STDERR "LINE($line)\n";

  if (header($line) eq "input-factors") {
    chomp(my $numfactors = <MOSES>);
    error("Joshua can't handle factors") unless $numfactors == 0;
  } elsif (header($line) eq "mapping") {
    ; # ignore
    
  } elsif (header($line) eq "ttable-file") {

	# Naively assume there are two grammars, the first non-glue, the
	# second glue

	my $grammarno = 0;
	my $total_numweights = 0;
    while (my $line = <MOSES>) {
      chomp($line);
	  next if $line =~ /^#/;
      last unless $line;
      my (undef,undef,undef,$numweights,$file) = split(' ',$line);
      push(@grammars,[$file,$numweights]);
	  $NUMFEATURES += $numweights;
	}

	for my $i (0..$#grammars) {
	  my $pair = $grammars[$i];
	  my ($file,$numweights) = @$pair;

	  my $grammar = convert_grammar($file, $i == 1, $total_numweights);
	  $total_numweights += $numweights;

	  if ($grammarno == 0) {
		print "tm_file=$grammar\n";
		print "tm_format=thrax\n";
		print "tm_owner=pt\n";
	  } else {
		print "glue_file=$grammar\n";
		print "glue_format=thrax\n";
		print "glue_owner=pt\n";
	  }

	  $grammarno++;
    }

  } elsif (header($line) eq "lmodel-file") {
	my ($type,undef,$order,$file) = split(' ', <MOSES>);

	if ($type == 0 or $type == 8 or $type == 9) {
	  # SRILM or KENLM
	  print "lm_file=$file\n";
	  print "order=$order\n";
	  print "use_left_equivalent_state=false\n";
	  print "use_right_equivalent_state=false\n";

	  if ($type == 0) {
		print "use_srilm=true\n";
	  } else {
		print "use_kenlm=true\n";
	  }
	  print "\n";
	} else {
	  error("Only language model types 0, 8, and 9 are supported");
	}
  } elsif (header($line) eq "ttable-limit") {
	chomp(my $limit = <MOSES>);

	warning("Joshua doesn't have a parameter corresponding to 'ttable-limit'");

  } elsif (header($line) eq "weight-l") {

	chomp(my $weight = <MOSES>);
	print "lm $weight\n\n";

  } elsif (header($line) eq "weight-t") {

	my $feature_no = 0;
	chomp(my $weight = <MOSES>);
	while ($weight) {
	  print "phrasemodel pt $feature_no $weight\n";
	  chomp($weight = <MOSES>);
	  $feature_no++;
	}
	print "\n";

  } elsif (header($line) eq "weight-w") {

	chomp(my $weight = <MOSES>);
	print "wordpenalty $weight\n\n";

  } elsif (header($line) eq "cube-pruning-pop-limit") {

	# Joshua does not appear to have an equivalent setting for this
	warning("no Joshua setting for cube-pruning-pop-limit");

  } elsif (header($line) eq "non-terminals") {

	# this is used for unknown words and for the source-side (if
	# unspecified in a rule); Joshua only supports its use for unknown
	# words
	print "default_non_terminal=X\n";
	print "goal_symbol=GOAL\n";
	print "\n";

  } elsif (header($line) eq "search-algorithm") {

	# TODO

  } elsif (header($line) eq "inputtype") {

	# TODO

  } elsif (header($line) eq "max-chart-span") {
	# Assume there are two limits, one for each grammar, and the
	# first is the non-glue grammar.  Joshua treats the glue grammar
	# specially (by passing a maximum span of -1), whereas Moses does
	# this in a more general fashion, not treating the glue grammar
	# specially, and using an (effectively unlimited) span of 1000.
	chomp(my $limit = <MOSES>);
	print "span_limit=$limit\n";
  }
}


print "top_n=1\n\n";
print "oovpenalty=-100\n";

# if (scalar @grammars == 1) {
#   open OUT, ">", "$outdir/glue-table" or die;
#   print OUT "[GOAL] ||| [X,1] ||| [X,1] ||| 1\n";

#   print "glue_file=$outdir/glue-table\n";
#   print "glue_format=thrax\n";
  
# }

close(MOSES);
close(JOSHUA);

######################################################################
## SUBROUTINES #######################################################
######################################################################

sub warning {
  my ($msg) = @_;

  print STDERR "* WARNING * $msg\n";
}

sub error {
  my ($msg) = @_;

  print STDERR "** FATAL ** $msg\n";
  exit;
}

sub header {
  my ($line) = @_;

  if ($line =~ (/^\[(\S+)\]/)) {
	return $1;
  }

  return "";
}

sub convert_grammar {
  my ($grammarfile,$is_glue,$num_weights_to_skip) = @_;

  if (-d $grammarfile) {
	$grammarfile =~ s/\.bin$//;
	error("Can't convert binarized format") if (! -e $grammarfile);
  }

  if (! -e $grammarfile and -e "$grammarfile.gz") { 
      $grammarfile = "$grammarfile.gz";
  }

  if ($grammarfile =~ /\.gz$/) {
    open GRAMMAR, "gzip -cd $grammarfile|" or error("can't read grammar '$grammarfile'");
  } else {
    open GRAMMAR, $grammarfile or error("can't read grammar '$grammarfile'");
  }

  my $filename = "$outdir/" . basename($grammarfile);


  if ( ($opts{g} and $is_glue) or ($opts{t} and ! $is_glue) ) {
	print STDERR "CONVERT_GRAMMAR($grammarfile, is_glue=$is_glue, skip=$num_weights_to_skip)\n";

	if ($filename =~ /\.gz$/) {
	  open OUT, "| gzip -9 > $filename" or error("can't write grammar to '$filename'");
	} else {
	  open OUT, ">", $filename or error("can't write grammar to '$filename'");
	}


	# Rules look like these:
    # <s> [X] ||| <s> [S] ||| 1 ||| ||| 0
    # [X][S] </s> [X] ||| [X][S] </s> [S] ||| 1 ||| 0-0 ||| 0
    # [X][S] [X][X] [X] ||| [X][S] [X][X] [S] ||| 2.718 ||| 0-0 1-1 ||| 0

	while (my $rule = <GRAMMAR>) {
	  chomp($rule);

	  # skip the rule with <s>
	  next if ($rule =~ /<s>/);

	  my $orig_rule = $rule;

	  my ($l1, $l2, $probs, $alignment) = split(/ *\|\|\| */, $rule, 4);

      # the </s> rule triggers a nonterminal change, which has a very
      # different format; (I'm not sure if this is the most general way
      # to handle this, and I think now)
	  if ($rule =~ /<\/s>/) {

		my @probs = map { transform($_) } (split(' ',$probs));
		if ($num_weights_to_skip) {
		  unshift(@probs, (0) x $num_weights_to_skip);
		}

		# append so as to have the same number
		while (scalar @probs < $NUMFEATURES) {
		  push(@probs, 0);
		}
		my $scores = join(" ", @probs);

		print OUT "[GOAL] ||| [X,1] ||| [X,1] ||| $scores\n";
		next;
	  }

	  # e.g., [X][S] </s> [X]
	  # l1tokens = ("[X][S]", "</s>", "[X]")
	  # l1nt = "[X]"
	  # l1rhs = "[X][S] </s>"
	  my (@l1tokens) = split(' ', $l1);
	  my $l1lhs = pop(@l1tokens);
	  my $l1rhs = join(" ",@l1tokens);
	  my (@l2tokens) = split(' ', $l2);
	  my $l2lhs = pop(@l2tokens);
	  my $l2rhs = join(" ",@l2tokens);

	  # make sure the LHSs match, since Joshua doesn't support
	  # non-matching ones; a non-match is expected for glue rules,
	  # since the target side switches to S
	  if ($l1lhs ne $l2lhs and ! $is_glue) {
		print STDERR "* WARNING: skipping '$rule' with non-matching lefthand sides\n";
		next;
	  }

	  # e.g., "[X][S] [X][X] [X]"
	  my (@l1nts);
	  while ($l1rhs =~ /\[(\S+?)\]\[(\S+?)\]/g) {
		my $source_symbol = $1;
		my $target_symbol = $2;

		if ($source_symbol ne $target_symbol and ! $is_glue) {
		  print STDERR "* WARNING: skipping '$rule' with non-matching rhsl labels ($source_symbol != $target_symbol)\n";
		  next;
		}

		push(@l1nts, get_symbol($target_symbol));
	  }

	  my (@l2nts);
	  while ($l2rhs =~ /\[(\S+?)\]\[(\S+?)\]/g) {
		my $source_symbol = $1;
		my $target_symbol = $2;

		if ($source_symbol ne $target_symbol and ! $is_glue) {
		  print STDERR "* WARNING: skipping '$rule' with non-matching rhsr labels ($source_symbol != $target_symbol)\n";
		  next;
		}

		push(@l2nts, get_symbol($target_symbol));
	  }

	  if (scalar @l1nts != scalar @l2nts) {
		print STDERR "* WARNING: nonterminal count mismatch on RHS\n";
		next;
	  }

	  if (scalar @l1nts == 1) {

		# unary rule
		$l1rhs =~ s/\[\w*?\]\[\w*?\]/[$l1nts[0],1]/;
		$l2rhs =~ s/\[\w*?\]\[\w*?\]/[$l2nts[0],1]/;

	  } elsif (scalar @l1nts == 2) {

		# binary rule
		$alignment =~ /(\d+)-(\d+) (\d+)-(\d+)/;
		if ($1 < $3 and $2 < $4) {
		  # straight rule
		  $l1rhs =~ s/\[\w*?\]\[\w*?\]/[$l1nts[0],1]/;
		  $l1rhs =~ s/\[\w*?\]\[\w*?\]/[$l1nts[1],2]/;
		  $l2rhs =~ s/\[\w*?\]\[\w*?\]/[$l2nts[0],1]/;
		  $l2rhs =~ s/\[\w*?\]\[\w*?\]/[$l2nts[1],2]/;
		} else {
		  # inverted rule
		  $l1rhs =~ s/\[\w*?\]\[\w*?\]/[$l1nts[0],1]/;
		  $l1rhs =~ s/\[\w*?\]\[\w*?\]/[$l1nts[1],2]/;
		  $l2rhs =~ s/\[\w*?\]\[\w*?\]/[$l2nts[1],2]/;
		  $l2rhs =~ s/\[\w*?\]\[\w*?\]/[$l2nts[0],1]/;
		}
	  } elsif (scalar @l1nts > 2) {
		warning("Skipping rule ($rule) with more than two nonterminals");
		next;
	  }

	  if ($l1rhs eq "" or $l2rhs eq "") {
		warning("skipping rule $orig_rule");
		next;
	  }

	  my @probs = map { transform($_) } (split(' ',$probs));
	  if ($num_weights_to_skip) {
		unshift(@probs, (0) x $num_weights_to_skip);
	  }

	  # append so as to have the same number
	  while (scalar @probs < $NUMFEATURES) {
		push(@probs, 0);
	  }
	  my $scores = join(" ", @probs);

	  my $lhs = ($l2lhs eq "[S]") ? "[GOAL]" : $l2lhs;
	  print OUT "$lhs ||| $l1rhs ||| $l2rhs ||| $scores\n";

	}
	close(OUT);
	close(GRAMMAR);
  }

  return $filename;
}

sub get_symbol {
  my ($symbol) = @_;

  return ($symbol eq "S") ? "GOAL" : $symbol;
}

sub select_symbol {
  my ($source, $target) = @_;

  return $target;
}

sub transform {
  my ($weight) = @_;

  return "99999" if ($weight == 0.0);
  
  # if ($weight eq "2.718") {
  # 	return $weight;
  # } else {
  # 	return -log($weight);
  # }

  return -log($weight);
}
