#!/usr/bin/perl -w
use Encode;
#### get the eng voc of the phrase tbl based on the input chinese

my $fcn_text=$ARGV[0]; #test cn sentences
my $fphrase_tbl=$ARGV[1];
my $feng_voc=$ARGV[2];
my $ftbl_out=$ARGV[3];


my %eng_voc=();

#add basic lm vocabulary here
$eng_voc{"<unk>"}=1;
$eng_voc{"<s>"}=1;
$eng_voc{"</s>"}=1;

@g_cn_sents=();
get_cn_sents($fcn_text,\@g_cn_sents);

#filter phrase tbl and get the english voc
open(FTM, $fphrase_tbl) or die "cannot open file $fphrase_tbl\n";
open(FOUT, ">$ftbl_out") or die "cannot open file $ftbl_out\n";
while(my $line=<FTM>){
    chomp($line);
    my @fds=split(/\s+\|{3}\s+/,$line);
    my $cn=$fds[1];
    my $eng=$fds[2];

    next if(filter_rule($cn,\@g_cn_sents)==1);
    print FOUT "$line\n";

    my @eng_wrds=split(/\s+/, $eng);
    foreach my $wrd (@eng_wrds){
	$wrd =~ s/^\s+//g;
	$wrd =~ s/\s+$//g;
	next if($wrd =~ m/^\[PHRASE,\d+\]$/);
	$eng_voc{$wrd}=1;
    }	
}
close(FTM);
close(FOUT);

### print the voc
open(FVOC, ">$feng_voc") or die "cannot open file $feng_voc\n";
foreach my $wrd (keys %eng_voc){
    #$wrd = encode("utf8", $wrd);
    print FVOC "$wrd\n";
}
close(FVOC);

#filter the rule through a voc tbl
sub filter_rule {
    my ($rule, $p_sents)=@_;
    #print STDERR "r1: $rule ||| ";

    $rule=add_escape($rule);
    #print STDERR "r2: $rule ||| ";

    $rule =~ s/\\\[PHRASE\\,\d+\\\]/\.\+/g; #ignore phrase tag
    #print STDERR "r3: $rule ||| ";

    my $res=1;
    foreach my $src_sent (@{$p_sents}){
    	if($src_sent =~ m/$rule/ ){ #if any src sent contains this rule, then retain the rule
		$res=0;
                last;
	}
    }
    #print STDERR "$res\n";
    return $res;
}


#input text may contain the special chars directly, we need to add \
sub add_escape {
    my ($text)=@_;
    $text =~ s/(\@|\.|\^|\$|\*|\+|\?|\[|\]|\{|\}|\(|\)|\<|\>|\/|\\|\||\`|\'|\"|\=|\-|\+|\,)/\\$1/g;
    return $text;
}

### get unigram voc tbl
sub get_cn_sents {
    my ($file, $p_sents)=@_;
    open(FILE, $file) or die "cannot open file $file\n";
    while(my $line=<FILE>){
	$line =~ s/<seg\s+id=\d+>//g;
	$line =~ s/<\/seg>//g;

        next if($line =~ m/^\s+$/); #blank line
        chomp($line);
        push(@{$p_sents}, $line);
    }
    close(FILE);
}



############################# not used

#filter the rule through a voc tbl
sub filter_rule_old {
    my ($rule, $p_tbl)=@_;
    my @wrds=split(/\s+/,$rule);
    foreach my $wrd ( @wrds ){
	next if($wrd =~ m/^\[PHRASE,\d\]$/); #ignore phrase tag
	if(not exists $p_tbl->{$wrd}){
		return 1;
	}
    }
    return 0;
}


### get unigram voc tbl
sub get_cn_voc_tbl {
    my ($file, $p_tbl)=@_;
    open(FILE, $file) or die "cannot open file $file\n";
    while(my $line=<FILE>){
        next if($line =~ m/^\s+$/); #blank line
        chomp($line);
        my @wrds=split(/\s+/,$line);
        foreach my $wrd ( @wrds ){
		$p_tbl->{$wrd}=1;   
	}
    }
    close(FILE);
}


######## not used
sub read_cn_voc_tbl {
    my ($file, $p_tbl)=@_;
    open(FILE, $file) or die "cannot open file $file\n";
    while(my $line=<FILE>){
        next if($line =~ m/^\s+$/); #blank line
        chomp($line);
        $p_tbl->{$line}=1;   
    }
    close(FILE);
}


