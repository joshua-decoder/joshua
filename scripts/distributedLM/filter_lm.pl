#!/usr/bin/perl -w

my $flm_in = $ARGV[0];
my $fvoc = $ARGV[1];
my $fout = $ARGV[2];

my %eng_voc=();
read_hash_tbl($fvoc, \%eng_voc);
my @tem = keys %eng_voc;

print STDERR "voc size is $#tem+1\n";

my $n_bow_nodes=0;
#filter LM
open(FLM, $flm_in) or die "cannot open file $flm_in\n";
open(FOUT, ">$fout") or die "cannot open file $fout\n";
my $order=-1;
my $count=0;
while(my $line=<FLM>){
   if($line =~ m/^\s+$/){ #blank line
       print FOUT "\n";
       next;
   }
   chomp($line);
   $line =~ s/^\s+//g;
   $line =~ s/\s+$//g;
   if($line =~ m/^\\(\d)-grams:$/){
	print STDERR "count of old order($order) is $count\n";
	print STDERR "num of bow nodes is $n_bow_nodes\n";
	$n_bow_nodes=0;
        $count=0;
	$order=$1;
	print FOUT "$line\n";
	print STDERR "order is $order\n";
        next;
   }
   my @fds=split(/\s+/,$line);
   if(@fds<$order+1){
      print FOUT "$line\n";
      next; #bad lines
   }
   if(@fds==$order+2){
	$n_bow_nodes++;
   }

   my $keep=1;
   for(my $i=1; $i<=$order; $i++){   
      #$fds[$i] =~ s/[[:^print:]]+//g; #remove all unprintable characters
      if(not exists $eng_voc{$fds[$i]}){
         $keep=0;
	 last;
      }
   }
 
   my $t_line = join(' ', @fds);
   #print STDERR "line: $t_line ||| keep: $keep\n"; 
   
   if($keep == 1){
      $count++;
      print FOUT "$line\n";
   }
}
print STDERR "count of old order($order) is $count\n";

close(FLM);
close(FOUT);


sub read_hash_tbl {
    my ($file, $p_tbl)=@_;
    open(FILE, $file) or die "cannot open file $file\n";
    while(my $line=<FILE>){
        next if($line =~ m/^\s+$/); #blank line
        chomp($line);
	$line =~ s/^\s+//g;
	$line =~ s/\s+$//g;
        $p_tbl->{"$line"}=1;   
    }
    close(FILE);
}


