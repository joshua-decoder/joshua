#!/usr/bin/env perl

# Takes a config file on STDIN, and replaces each of the file arguments with those found on the
# command line.  Parameters not found in the config file (and thus not replaceable) are appended to
# the end.
#
# Usage:
#
#    cat joshua.config | copy-config.pl -param` value -param2 "multi-word value" ...

use strict;
use warnings;

# Step 1. process command-line arguments for key/value pairs.  The keys are matched next to the
# config file and the configfile values replaced with those found on the command-line.

my (%params);
while (my $key = shift @ARGV) {
  # make sure the parameter has a leading dash
  if ($key !~ /^-/) {
    print STDERR "* FATAL: invalid command-line argument '$key'\n";
    exit 1;
  }

  # remove leading dash
  $key =~ s/^-+//g;

  # get the value and store the pair
  my $value = shift(@ARGV);

  $params{normalize_key($key)} = $value;
}

# Step 2.  Now read through the config file.

while (my $line = <>) {
  if ($line =~ /=/) {
    # split on equals
    my ($key,$value) = split(/\s*=\s*/, $line, 2);

    # remove leading and trailing spaces
    $key =~ s/^\s+//g;
    $value =~ s/\s+$//g;

    # if the parameter was found on the command line, print out its replaced value
    my $norm_key = normalize_key($key);
    if (exists $params{$norm_key}) {
      print "$key = " . $params{$norm_key} . "\n";
      delete $params{$norm_key};
    } else {
      # otherwise, print out the original line
      print $line;
    }

  } else {
    print $line;
  }
}

# print out the remaining keys for appending to the end of the file
if (scalar(keys(%params))) {
  print $/;
  foreach my $key (keys %params) {
    print STDERR "* WARNING: no key '$key' found in config file (appending to end)\n";
    print "$key = $params{$key}\n";
  }
}

sub normalize_key {
  my ($key) = @_;

  $key =~ s/[-_]//g;
  $key = lc $key;

#   print STDERR "** KEY($_[0]) -> $key\n";
  return $key;
}
