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
getopts("i:d:",\%opts);

my $moses_ini_file = $opts{i} || usage();
my $outdir         = $opts{d} || "joshua";

sub usage {
  print "Usage: moses2joshua.pl -i moses.ini [-d outputdir=joshua]\n";
  exit;
}

print STDERR "using outdir '$outdir', moses file = $moses_ini_file\n";

# The number of features contained in each grammar found.  This is used to compute feature names
# for Joshua
my (@num_features);

open MOSES, $moses_ini_file or die "can't find moses file '$moses_ini_file'";

system("mkdir","-p",$outdir) unless -d $outdir;
open JOSHUA, ">", "$outdir/joshua.config" or die;
open WEIGHTS, ">", "$outdir/weights" or die;

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

    my $grammarno = 0;
    while (my $line = <MOSES>) {
      chomp($line);
      next if $line =~ /^#/;
      last unless $line;
      my (undef,undef,undef,$numweights,$file) = split(' ',$line);
      push(@num_features, $numweights);
      my $grammar = convert_grammar($file);
      
      my $span_limit = get_span_limit($moses_ini_file, $grammarno);

      print JOSHUA "tm = thrax owner$grammarno $span_limit $grammar\n";
      $grammarno++;
    }

  } elsif (header($line) eq "lmodel-file") {
    my ($type,undef,$order,$file) = split(' ', <MOSES>);

    if ($type == 0 or $type == 8 or $type == 9) {
      print JOSHUA "lm = kenlm $order false false 100 $file\n";
    } else {
      error("Only language model types 0, 8, and 9 are supported");
    }
  } elsif (header($line) eq "ttable-limit") {
    chomp(my $limit = <MOSES>);

    warning("Joshua doesn't have a parameter corresponding to 'ttable-limit'");

  } elsif (header($line) eq "weight-l") {

    chomp(my $weight = <MOSES>);
    print WEIGHTS "lm_0 $weight\n";

  } elsif (header($line) eq "weight-t") {

    # This takes a bit of thinking.  Moses lists grammars one by one; each grammar lists the number
    # of features it has.  The weights are then listed as a block and mapped to the corresponding
    # file and index based on these counts and the respective ordering of the files and weights.
    # Joshua (in its sparse feature implementation) instead names each feature according to the
    # pattern "tm_OWNER_INDEX".  So here we map from these positions in the Moses file to Joshua
    # names.

    my $grammarno = 0;
    my $index = 0;

    chomp(my $weight = <MOSES>);
    while ($weight) {
      print WEIGHTS "tm_owner${grammarno}_${index} $weight\n";
      # If we reach the number of features in the current grammar, increment the grammar and reset
      # the index.
      if ($index >= $num_features[$grammarno] - 1) {
        $grammarno++;
        $index = 0;
      } else {
        $index++;
      }
      chomp($weight = <MOSES>);
    }
    print "\n";

  } elsif (header($line) eq "weight-w") {

    chomp(my $weight = <MOSES>);
    print WEIGHTS "WordPenalty $weight\n";

  } elsif (header($line) eq "cube-pruning-pop-limit") {

    chomp(my $limit = <MOSES>);

    # Joshua does not appear to have an equivalent setting for this
    print JOSHUA "pop-limit = $limit\n";

  } elsif (header($line) eq "non-terminals") {

    # this is used for unknown words and for the source-side (if
    # unspecified in a rule); Joshua only supports its use for unknown
    # words
    print JOSHUA "default-non-terminal=X\n";
    print JOSHUA "goal-symbol=GOAL\n";
    print JOSHUA "\n";

  } elsif (header($line) eq "search-algorithm") {

    # TODO

  } elsif (header($line) eq "inputtype") {

    # TODO

  }
}

print JOSHUA "top-n=1\n\n";
print JOSHUA "weights-file = weights\n";

print WEIGHTS "OOVPenalty -100\n";

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
  my ($grammarfile) = @_;

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

    # e.g., "[X][S] [X][X] [X]"
    my (@l1nts);
    while ($l1rhs =~ /\[(\S+?)\]\[(\S+?)\]/g) {
      my $source_symbol = $1;
      my $target_symbol = $2;

      push(@l1nts, get_symbol($target_symbol));
    }

    my (@l2nts);
    while ($l2rhs =~ /\[(\S+?)\]\[(\S+?)\]/g) {
      my $source_symbol = $1;
      my $target_symbol = $2;

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
    my $scores = join(" ", @probs);

    my $lhs = ($l2lhs eq "[S]") ? "[GOAL]" : $l2lhs;
    print OUT "$lhs ||| $l1rhs ||| $l2rhs ||| $scores\n";

  }
  close(OUT);
  close(GRAMMAR);

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

# Reads the moses config file to look for the span limit for grammar i (0-indexed).
sub get_span_limit {
  my ($config_file, $index) = @_;

  open READ, $config_file or die "can't find config file '$config_file'";
  while (my $line = <READ>) {
    if ($line =~ /max-chart-span/) {
      # Burn through a number of lines until we get to the one we need.  This assumes there are no
      # intervening comments or blank lines.
      for (my $i = 0; $i < $index; $i++) {
        my $t = <READ>;
      }
      last;
    }
  }
  chomp(my $max_span = <READ>);

  close(READ);
  return $max_span;
}
