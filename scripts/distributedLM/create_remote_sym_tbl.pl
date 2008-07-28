#!/usr/bin/perl -w

$fvoc=$ARGV[0]; #LM&TM&globalsymbol VOC FILE
$fsym=$ARGV[1]; #output
$start_id=$ARGV[2]; #lm start id
$start_id=10000 if (not defined $start_id);

my %g_sym_tbl=();
my $g_id=$start_id;

open(FSYM, ">$fsym") or die "cannot open file $fsym\n";
process_one_file($fvoc);
close(FSYM);

sub process_one_file{
	my ($file)=@_;
	open(FILE, $file) or die "cannot open file $file\n";
	while(my $line=<FILE>){
		chomp($line);
		next if($line =~ m/^\s+$/);
		my @fds=split(/\s+/, $line);
		if(exists $g_sym_tbl{$fds[0]}){
			print STDERR "duplicate symbol: $line\n";
		}else{
			print FSYM "$fds[0] $g_id\n";
			$g_id++;
			$g_sym_tbl{$fds[0]}=1;
		}
	}
	close(FILE);
}

