#!/usr/bin/perl -w

# This script lowercases just the leaves of a tree, represented in
# standard PTB form.

use strict;

binmode(STDIN, ":utf8");
binmode(STDOUT, ":utf8");

while (<>) {
  s/(\S+?)\)/lc $1 . ")"/ge;
  print;
}
