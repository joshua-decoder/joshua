#!/usr/bin/perl -w
#input is filtered grammar file
#output is english vocabulary
my %eng_voc=();


#add lm vocabulary here
$eng_voc{"<unk>"}=1;
$eng_voc{"<s>"}=1;
$eng_voc{"</s>"}=1;
$eng_voc{"-pau-"}=1;


while(my $line=<>){
    chomp($line);
    my @fds=split(/\s+\|{3}\s+/,$line);
    my $eng=$fds[2];
    my @eng_wrds=split(/\s+/, $eng);
    foreach my $wrd (@eng_wrds){
	$wrd =~ s/^\s+//g;
	$wrd =~ s/\s+$//g;
	if($wrd =~ m/^\[[a-zA-Z]+,\d+\]$/){ #[PHRASE,1]
	    next;
	}
	$eng_voc{$wrd}=1;
    }	
}

foreach my $wrd (keys %eng_voc){
    print "$wrd\n";
}
