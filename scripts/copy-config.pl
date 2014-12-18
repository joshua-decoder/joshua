#!/usr/bin/env perl

# Takes a config file on STDIN, and replaces each of the file arguments with those found on the
# command line.  Parameters not found in the config file (and thus not replaceable) are appended to 
# the end.
#
#    cat joshua.config | copy-config.pl -param1 value -param2 "multi-word value" ...
#
# Some parameters can take options.  For example, there are multiple permitted "tm" lines.  If you
# want to specify which one to replace, you can add "/owner" after the name.  For example,
#
#    cat joshua.config | copy-config.pl -tm/pt "tm = thrax pt 12 /path/to/grammar"
#
# This will ensure that only the tm line with the "pt" owner gets replaced.  Note that if there is
# more than one, only the first one will be replaced.

use strict;
use warnings;

# Step 1. process command-line arguments for key/value pairs.  The keys are matched next to the
# config file and the configfile values replaced with those found on the command-line.

my (%params,%restrictions);
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

    my $norm_key = normalize_key($key);

    if ($norm_key eq "tm") {
      my (undef,$owner) = split(' ', $value);
      $norm_key = "$norm_key/$owner" if (exists $params{"$norm_key/$owner"});
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

