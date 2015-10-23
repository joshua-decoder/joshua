#!/usr/bin/env perl

# Takes a config file on STDIN, and replaces each of the file arguments with those found on the
# command line.  Parameters not found in the config file (and thus not replaceable) are appended to 
# the end.
#
#    cat joshua.config | copy-config.pl -param1 value -param2 "multi-word value" ...
#
# Some parameters can take options.  For example, there are multiple permitted "tm" lines.  If you
# want to specify which one to replace, you can add "/N" after the name, where N is the 0-indexed
# index of the grammar.  For example,
#
#    cat joshua.config | copy-config.pl -tm0/path /path/to/grammar -tm0/owner pt
#
# This will ensure that only the first tm line gets updated.
#
# Most keys are replacement keys: specifying a value will replace what's found in the config
# file. The only exception is -feature-function, instances of which are appended to the output.
# Feature functions can't be deleted from the config file.
#
# Weights

use strict;
use warnings;

# Step 1. process command-line arguments for key/value pairs.  The keys are matched next to the
# config file and the configfile values replaced with those found on the command-line.

my (%params,%weights,%features);
while (my $key = shift @ARGV) {
  # make sure the parameter has a leading dash
  if ($key !~ /^-/) {
    print STDERR "* FATAL: invalid command-line argument '$key'\n";
    exit 1;
  }

  # remove leading dash
  $key =~ s/^-+//g;
  $key = normalize_key($key);

  # get the value and store the pair
  my $value = shift(@ARGV);

  # -feature-function lines are gathered, other keys can be present only once
  if ($key eq "featurefunction") {
    $features{$value} = $value;
  } elsif ($key eq "weights") {
    my @tokens = split(' ', $value);
    for (my $i = 0; $i < @tokens; $i += 2) {
      $weights{$tokens[$i]} = $tokens[$i+1];
    }
  } else {
    $params{$key} = $value;
  }
}

# Step 2.  Now read through the config file.

my @weights_order;
my $tm_index = -1;
while (my $line = <>) {
  if ($line =~ /^\s*$/ or $line =~ /^#/) {
    # Comments, empty lines
    print $line;

  } elsif ($line =~ /=/) {
    # Regular configuration variables.

    # split on equals
    my ($key,$value) = split(/\s*=\s*/, $line, 2);

    # remove leading and trailing spaces
    $key =~ s/^\s+//g;
    $value =~ s/\s+$//g;

    my $norm_key = normalize_key($key);

    # TMs get special treatment. We parse the line (supporting old format and new keyword format),
    # and then compare to command-line args to see what gets updated
    if ($norm_key =~ /^tm/) {
      $tm_index++;

      # get the hash of tm values from the config file
      my $tm_hash = parse_tm_line($value);

      # Delete TM lines if they've been requested to be deleted
      if (exists $params{"tm${tm_index}"} and $params{"tm${tm_index}"} eq "DELETE") {
        delete $params{"tm${tm_index}"};
        next;
      }

      # check if each one was passed as a command-line argument, and if so, retrieve its new value
      foreach my $tmkey (keys %$tm_hash) {
        my $concat = "tm${tm_index}/${tmkey}";
        if (exists $params{$concat}) {
          $tm_hash->{$tmkey} = $params{$concat};
          delete $params{$concat};
        }
      }
      # write out the new line (using new keyword format always)
      $params{$norm_key} = $tm_hash->{type};
      foreach my $tmkey (keys %$tm_hash) {
        next if $tmkey eq "type";
        $params{$norm_key} .= " -$tmkey $tm_hash->{$tmkey}";
      }
    }

    # If an exact feature function line is in the config file, delete
    # it from the command-line arguments so it doesn't get printed
    # later. All features not found in the config file are appended.
    if ($norm_key eq "featurefunction" and exists $features{$value}) {
      delete $features{$value};
    }

    # if the parameter was found on the command line, print out its replaced value
    if (exists $params{$norm_key}) {
      print "$key = " . $params{$norm_key} . "\n";

      # Deleting the parameter means it will only match the first time.  Useful for duplicated keys
      # (like multiple "tm = ..." lines)
      delete $params{$norm_key};
    } else {
      # otherwise, print out the original line
      print $line;
    }

  } else {
    # Weights. Save these to print at the end, just to keep things neat.
    chomp($line);
    my ($name, $value) = split(' ', $line);
    $weights{$name} = $value unless exists $weights{$name};
    push(@weights_order, $name);
  }
}

# print out the remaining keys for appending to the end of the file
if (scalar(keys(%params))) {
  print $/;
  foreach my $key (keys %params) {
    next if $key =~ /^tm/; # skip unused tm flags
    print STDERR "* WARNING: no key '$key' found in config file (appending to end)\n";
    print "$key = $params{$key}\n";
  }
}

# print out the feature functions
map { print "feature-function = $_\n" } (keys %features);
print $/;

# Print out the weights
foreach my $weight (@weights_order) {
  print "$weight $weights{$weight}\n";
  delete $weights{$weight};
}
foreach my $weight (keys %weights) {
  print "$weight $weights{$weight}\n";
}

# Remove hyphens and underscores, lowercase
sub normalize_key {
  my ($key) = @_;

  $key =~ s/[-_]//g;
  $key = lc $key;

#   print STDERR "** KEY($_[0]) -> $key\n";
  return $key;
}

# Produces a {key => value} hash from the TM line, supporting both the old format:
# 
#   tm = thrax pt 0 /path/to/grammar.gz
#
# and the new one
#
#   tm = thrax -owner pt -maxspan 0 -path /path/to/grammar.gz
#
sub parse_tm_line {
  my ($line) = @_;

  # line might still have keyword on it
  $line =~ s/^tm = // if ($line =~ /^tm = /);

  my %hash;
  my @tokens = split(' ', $line);
  $hash{type} = shift(@tokens);
  if ($tokens[0] =~ /^-/) {
    while (@tokens) {
      my $key = shift(@tokens);
      my $value = shift(@tokens);
      $key =~ s/^-//;
      $hash{$key} = $value;
    }
  } else {
    $hash{owner} = shift(@tokens);
    $hash{maxspan} = shift(@tokens);
    $hash{path} = shift(@tokens);
  } 

  return \%hash;
}
