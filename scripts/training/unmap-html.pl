#!/usr/bin/perl

# Remove HTML codes from data.

binmode(STDIN,  ":utf-8");
binmode(STDOUT, ":utf-8");

my %map = (
  "&#39;" => "'",
  "&#44;" => ",",
  "&amp;" => "&",
  "&gt;"  => ">",
  "&lt;"  => "<",
  "&quot;" => "\"",
  "&#257;" => "ā",
  "&#257:" => "ā",
  "&#257 " => "ā",
  "&#7751;" => "Ṇ",
  "&#7751:" => "Ṇ",
  "&#7779;" => "Ṣ",
  "&#7779:" => "Ṣ",
  "&#;" => "",
);

while (my $line = <>) {
  foreach my $key (keys %map) {
    $line =~ s/$key/$map{$key}/g;
  }

  print $line;
}
