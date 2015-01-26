#!/usr/bin/perl
# 2011-07-14 Matt Post <post@cs.jhu.edu>

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

sub usage {
  print "Usage: cat moses.ini | moses2joshua.pl > joshua.config\n";
  exit;
}

# The number of features contained in each grammar found.  This is used to compute feature names
# for Joshua
my (@num_features, @WEIGHTS);

my $grammar_no = 0;
my @span_limits;

while (my $line = <STDIN>) {
  chomp($line);
  next if $line =~ /^#/;

#  print STDERR "LINE($line)\n";

  if (header($line) eq "input-factors") {
    chomp(my $numfactors = <>);
    error("Joshua can't handle factors") unless $numfactors == 0;
  } elsif (header($line) eq "mapping") {
    ; # ignore
    
  } elsif (header($line) eq "ttable-file") {

    my $grammarno = 0;
    while (my $line = <>) {
      chomp($line);
      next if $line =~ /^#/;
      last unless $line;
      my (undef,undef,undef,$numweights,$file) = split(' ',$line);
      push(@num_features, $numweights);
      my $grammar = convert_grammar($file);
      
      my $span_limit = $span_limits[$grammarno];

      print "tm = thrax owner$grammarno $span_limit $grammar\n";
      $grammarno++;
    }

  } elsif (header($line) eq "lmodel-file") {
    my ($type,undef,$order,$file) = split(' ', <>);

    if ($type == 0 or $type == 8 or $type == 9) {
      print  "lm = kenlm $order false false 100 $file\n";
    } else {
      error("Only language model types 0, 8, and 9 are supported");
    }
  } elsif (header($line) eq "ttable-limit") {
    chomp(my $limit = <>);

    warning("Joshua doesn't have a parameter corresponding to 'ttable-limit'");

  } elsif (header($line) eq "weight-l") {

    chomp(my $weight = <>);
    push @WEIGHTS, "lm_0 $weight\n";

  } elsif (header($line) eq "weight-t") {

    # This takes a bit of thinking.  Moses lists grammars one by one; each grammar lists the number
    # of features it has.  The weights are then listed as a block and mapped to the corresponding
    # file and index based on these counts and the respective ordering of the files and weights.
    # Joshua (in its sparse feature implementation) instead names each feature according to the
    # pattern "tm_OWNER_INDEX".  So here we map from these positions in the Moses file to Joshua
    # names.

    my $grammarno = 0;
    my $index = 0;

    chomp(my $weight = <>);
    while ($weight) {
      push @WEIGHTS, "tm_owner${grammarno}_${index} $weight\n";
      # If we reach the number of features in the current grammar, increment the grammar and reset
      # the index.
      if ($index >= $num_features[$grammarno] - 1) {
        $grammarno++;
        $index = 0;
      } else {
        $index++;
      }
      chomp($weight = <>);
    }
    print "\n";

  } elsif (header($line) eq "weight-w") {

    chomp(my $weight = <>);
    print "feature-function = WordPenalty\n";
    push @WEIGHTS, "WordPenalty $weight\n";

  } elsif (header($line) eq "max-chart-span") {
    while (my $line = <>) {
      chomp($line);
      last if $line eq "";
      push @span_limits, $line;
    }

  } elsif (header($line) eq "weight") {
    while (my $line = <>) {
      chomp($line);
      last if $line eq "";
      my ($name, $value) = split(' ', $line, 2);
      my $num = $name; $num =~ s/^.*(\d+)=$/$1/;
      $name =~ s/(.*)\d+=$/$1/;
      if ($name eq "LM") {
        push @WEIGHTS, "lm_$num " . ($value * 2.3024448269);  # natural log(10)
      } elsif ($name =~ /^TranslationModel/) {
        my @weights = split(' ', $value);
        for (my $i = 0; $i < @weights; $i++) {
          push @WEIGHTS, "tm_owner${num}_${i} $weights[$i]";
        }
      } elsif ($name eq "UnknownWordPenalty") {
        push @WEIGHTS, "OOVPenalty " . ($value);
      } elsif ($name eq "WordPenalty") {
        push @WEIGHTS, "WordPenalty " . ($value / 0.4342944819032518);
      } else {
        push @WEIGHTS, "$name $value";
      }
    }

  } elsif (header($line) eq "feature") {
    while ($line = <>) {
      chomp($line);
      last if $line eq "";
      my ($key, @rest) = split(' ', $line);
      if ($key eq "UnknownWordPenalty") {
        print "feature-function = OOVPenalty\n";
      } elsif ($key eq "WordPenalty") {
        print "feature-function = WordPenalty\n";
      } elsif ($key eq "PhrasePenalty") {
        print "feature-function = PhrasePenalty -owner owner0\n";
      } elsif ($key eq "Distortion") {
        print "feature-function = Distortion\n";
      } elsif ($key =~ /^PhraseDictionary/) {
        my $grammar_file;
        my $table_limit = 20;
        foreach my $token (@rest) {
          if ($token =~ /^path/) {
            $token =~ s/^path=//;
            $grammar_file = $token;
          } elsif ($token =~ /^table-limit/) {
            $token =~ s/^table-limit=//;
            $table_limit = $token;
          }
        }
        my $span_limit = $span_limits[$grammar_no] || 1000;
        my $owner = "moses";
        print "tm = $owner owner${grammar_no} $span_limit $grammar_file\n";
        print "num_translation_options = $table_limit\n";
        $grammar_no++;

      } elsif ($key eq "KENLM") {
        my $str = join(" ", @rest);
        my $order = $str; $order =~ s/.*order=(\d+).*/$1/;
        my $path  = $str; $path  =~ s/.*path=(\S+).*/$1/;
        print "lm = kenlm $order true false 100 $path\n";
      }
    }

  } elsif (header($line) eq "cube-pruning-pop-limit") {

    chomp(my $limit = <>);

    # Joshua does not appear to have an equivalent setting for this
    print "pop-limit = $limit\n";

  } elsif (header($line) eq "non-terminals") {

    # this is used for unknown words and for the source-side (if
    # unspecified in a rule); Joshua only supports its use for unknown
    # words
    print "default-non-terminal = X\n";
    print "goal-symbol = GOAL\n";
    print "\n"; 

  } elsif (header($line) eq "distortion" or header($line) eq "distortion-limit") {
    chomp(my $limit = <>);

    print "reordering-limit = $limit\n";

  } elsif (header($line) eq "search-algorithm") {

    # TODO

  } elsif (header($line) eq "inputtype") {

    # TODO

  }
}

print "top-n = 1\n\n";
print "mark-oovs = false\n";

print "\n# WEIGHTS\n\n";
foreach my $weight (@WEIGHTS) {
  print $weight . $/;
}


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
