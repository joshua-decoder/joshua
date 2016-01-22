#!/usr/bin/perl

# This file runs GIZA++ in both directions.  It was taken from the
# Moses decoder script train-model.perl.

use strict;
use warnings;
use Getopt::Long;

my $JOSHUA = $ENV{JOSHUA};

my $ZCAT = "gzip -cd";
my $BZCAT = "bzcat";
my $SYMAL = "$JOSHUA/ext/symal/symal";
my $GIZA2BAL = "$JOSHUA/ext/symal/giza2bal.pl";

my ($_F,$_E,$_ROOT_DIR,$_CORPUS,$_PARALLEL);
my ($_HMM_ALIGN,$_FINAL_ALIGNMENT_MODEL,$_GIZA_EXTENSION,$_DICTIONARY,$_MGIZA,$_MGIZA_CPUS,$_GIZA_E2F,$_GIZA_F2E,$_GIZA_OPTION,$_ONLY_PRINT_GIZA,$_ALIGNMENT);

my $BINDIR = "$JOSHUA/bin";

my $retval = GetOptions(
  "e=s"   	 	  => \$_E,
  "f=s"  	 	  => \$_F,
  "root-dir=s"    => \$_ROOT_DIR,
  "corpus=s"      => \$_CORPUS,
  "parallel!"     => \$_PARALLEL,
  "bindir=s"      => \$BINDIR,
  "merge=s"   => \$_ALIGNMENT,  # union, {intersect, grow, srctotgt, tgttosrc}-{diag,final,final-and}, default "grow-diag-final"
	);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

my $___ROOT_DIR = ".";
$___ROOT_DIR = $_ROOT_DIR if $_ROOT_DIR;
my $___MODEL_DIR = $___ROOT_DIR."/model";
my $___ALIGNMENT_FILE = "$___MODEL_DIR/aligned";

# check the final-alignment-model switch
my $___FINAL_ALIGNMENT_MODEL = undef;
$___FINAL_ALIGNMENT_MODEL = 'hmm' if $_HMM_ALIGN;
$___FINAL_ALIGNMENT_MODEL = $_FINAL_ALIGNMENT_MODEL if $_FINAL_ALIGNMENT_MODEL;

die("ERROR: --final-alignment-model can be set to '1', '2', 'hmm', '3', '4' or '5'")
	unless (!defined($___FINAL_ALIGNMENT_MODEL) or $___FINAL_ALIGNMENT_MODEL =~ /^(1|2|hmm|3|4|5)$/);

my $___GIZA_EXTENSION = 'A3.final';
if(defined $___FINAL_ALIGNMENT_MODEL) {
  $___GIZA_EXTENSION = 'A1.5' if $___FINAL_ALIGNMENT_MODEL eq '1';
  $___GIZA_EXTENSION = 'A2.5' if $___FINAL_ALIGNMENT_MODEL eq '2';
  $___GIZA_EXTENSION = 'Ahmm.5' if $___FINAL_ALIGNMENT_MODEL eq 'hmm';
}
$___GIZA_EXTENSION = $_GIZA_EXTENSION if $_GIZA_EXTENSION;

my $MGIZA_MERGE_ALIGN = "$BINDIR/merge_alignment.py";
my $GIZA;
if(!defined $_MGIZA ){
  $GIZA = "$BINDIR/GIZA++";
  print STDERR "Using single-thread GIZA\n";
}
else {
  $GIZA = "$BINDIR/mgizapp";
  print STDERR "Using multi-thread GIZA\n";	
  if (!defined($_MGIZA_CPUS)) {
	$_MGIZA_CPUS=4;
  }
  die("ERROR: Cannot find $MGIZA_MERGE_ALIGN") unless (-x $MGIZA_MERGE_ALIGN);
}
my $SNT2COOC = "$BINDIR/snt2cooc.out"; 
my $MKCLS = "$BINDIR/mkcls";

my $___F = $_F;
my $___E = $_E;

my $___CORPUS_DIR  = $___ROOT_DIR."/corpus";
my $___CORPUS      = $_CORPUS;

my $___VCB_E = $___CORPUS_DIR."/".$___E.".vcb";
my $___VCB_F = $___CORPUS_DIR."/".$___F.".vcb";

# GIZA generated files
my $___GIZA = $___ROOT_DIR."/giza";
my $___GIZA_E2F = $___GIZA.".".$___E."-".$___F;
my $___GIZA_F2E = $___GIZA.".".$___F."-".$___E;
$___GIZA_E2F = $_GIZA_E2F if $_GIZA_E2F;
$___GIZA_F2E = $_GIZA_F2E if $_GIZA_F2E;
my $___GIZA_OPTION = "";
$___GIZA_OPTION = $_GIZA_OPTION if $_GIZA_OPTION;

# alignment heuristic
my $___ALIGNMENT = "grow-diag-final";
$___ALIGNMENT = $_ALIGNMENT if $_ALIGNMENT;
my $___NOTE_ALIGNMENT_DROPS = 1;

my $___LEXICAL_WEIGHTING = 1;

my $___PARTS = 1;
my $___DIRECTION = 0;

# don't fork
my $___NOFORK = !defined $_PARALLEL;

my $___ONLY_PRINT_GIZA = 0;
$___ONLY_PRINT_GIZA = 1 if $_ONLY_PRINT_GIZA;


&prepare();
&run_giza();
&word_align();


######################################################################
## SUBROUTINES #######################################################
######################################################################

sub run_giza {
  return &run_giza_on_parts if $___PARTS>1;

  print STDERR "(2) running giza @ ".`date`;
  if ($___DIRECTION == 1 || $___DIRECTION == 2 || $___NOFORK) {
	&run_single_giza($___GIZA_F2E,$___E,$___F,
					 $___VCB_E,$___VCB_F,
					 $___CORPUS_DIR."/$___F-$___E-int-train.snt")
	    unless $___DIRECTION == 2;
	&run_single_giza($___GIZA_E2F,$___F,$___E,
					 $___VCB_F,$___VCB_E,
					 $___CORPUS_DIR."/$___E-$___F-int-train.snt")
	    unless $___DIRECTION == 1;
  } else {
	my $pid = fork();
	if (!defined $pid) {
	  die "ERROR: Failed to fork";
	}
	if (!$pid) { # i'm the child
	  &run_single_giza($___GIZA_F2E,$___E,$___F,
					   $___VCB_E,$___VCB_F,
					   $___CORPUS_DIR."/$___F-$___E-int-train.snt");
	  exit 0; # child exits
	} else { #i'm the parent
	  &run_single_giza($___GIZA_E2F,$___F,$___E,
					   $___VCB_F,$___VCB_E,
					   $___CORPUS_DIR."/$___E-$___F-int-train.snt");
	}
	printf "Waiting for second GIZA process...\n";
	waitpid($pid, 0);
  }
}


sub prepare {
  print STDERR "(1) preparing corpus @ ".`date`;
  safesystem("mkdir -p $___CORPUS_DIR") or die("ERROR: could not create corpus dir $___CORPUS_DIR");
  
  my $corpus = $___CORPUS;

  my $VCB_F, my $VCB_E;

  if ($___NOFORK) {
	
	&make_classes($corpus.".".$___F,$___VCB_F.".classes");
	&make_classes($corpus.".".$___E,$___VCB_E.".classes");
	
	$VCB_F = &get_vocabulary($corpus.".".$___F,$___VCB_F);
	$VCB_E = &get_vocabulary($corpus.".".$___E,$___VCB_E);
	
	&numberize_txt_file($VCB_F,$corpus.".".$___F,
						$VCB_E,$corpus.".".$___E,
						$___CORPUS_DIR."/$___F-$___E-int-train.snt");
	
	&numberize_txt_file($VCB_E,$corpus.".".$___E,
						$VCB_F,$corpus.".".$___F,
						$___CORPUS_DIR."/$___E-$___F-int-train.snt");
  } 
  else {
	print "Forking...\n";
	my $pid = fork();
	die "ERROR: couldn't fork" unless defined $pid;
	if (!$pid) {
	  &make_classes($corpus.".".$___F,$___VCB_F.".classes");
	  exit 0;
	} # parent
	my $pid2 = fork();
	die "ERROR: couldn't fork again" unless defined $pid2;
	if (!$pid2) { #child
	  &make_classes($corpus.".".$___E,$___VCB_E.".classes");
	  exit 0;
	}
	
	$VCB_F = &get_vocabulary($corpus.".".$___F,$___VCB_F);
	$VCB_E = &get_vocabulary($corpus.".".$___E,$___VCB_E);
	
	&numberize_txt_file($VCB_F,$corpus.".".$___F,
						$VCB_E,$corpus.".".$___E,
						$___CORPUS_DIR."/$___F-$___E-int-train.snt");
	
	&numberize_txt_file($VCB_E,$corpus.".".$___E,
						$VCB_F,$corpus.".".$___F,
						$___CORPUS_DIR."/$___E-$___F-int-train.snt");
	printf "Waiting for mkcls processes to finish...\n";
	waitpid($pid2, 0);
	waitpid($pid, 0);
  }

  if (defined $_DICTIONARY)
  {
	my $dict= &make_dicts_files($_DICTIONARY, $VCB_F,$VCB_E,
								$___CORPUS_DIR."/gizadict.$___E-$___F",
								$___CORPUS_DIR."/gizadict.$___F-$___E");
	if (not $dict)
	{
	  print STDERR "WARNING: empty dictionary\n";
	  undef $_DICTIONARY;
	}
  }
}

sub word_align {

  print STDERR "(3) generate word alignment @ ".`date`;
  my (%WORD_TRANSLATION,%TOTAL_FOREIGN,%TOTAL_ENGLISH);
  print STDERR "Combining forward and inverted alignment from files:\n";
  print STDERR "  $___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.{bz2,gz}\n";
  print STDERR "  $___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.{bz2,gz}\n";

  ### build arguments for giza2bal.pl
  my($__ALIGNMENT_CMD,$__ALIGNMENT_INV_CMD);
  
  if (-e "$___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.bz2"){
	$__ALIGNMENT_CMD="<($BZCAT $___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.bz2)";
  } elsif (-e "$___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.gz") {
	$__ALIGNMENT_CMD="<($ZCAT $___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.gz)";
  } else {
	die "ERROR: Can't read $___GIZA_F2E/$___F-$___E.$___GIZA_EXTENSION.{bz2,gz}\n";
  }
  
  if ( -e "$___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.bz2"){
	$__ALIGNMENT_INV_CMD="<($BZCAT $___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.bz2)";
  }elsif (-e "$___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.gz"){
	$__ALIGNMENT_INV_CMD="<($ZCAT $___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.gz)";
  }else{
	die "ERROR: Can't read $___GIZA_E2F/$___E-$___F.$___GIZA_EXTENSION.{bz2,gz}\n\n";
  }
  
  safesystem("mkdir -p $___MODEL_DIR") or die("ERROR: could not create dir $___MODEL_DIR");
  
  #build arguments for symal
  my($__symal_a)="";
  $__symal_a="union" if $___ALIGNMENT eq 'union';
  $__symal_a="intersect" if $___ALIGNMENT=~ /intersect/;
  $__symal_a="grow" if $___ALIGNMENT=~ /grow/;
  $__symal_a="srctotgt" if $___ALIGNMENT=~ /srctotgt/;
  $__symal_a="tgttosrc" if $___ALIGNMENT=~ /tgttosrc/;
  
  
  my($__symal_d,$__symal_f,$__symal_b);
  ($__symal_d,$__symal_f,$__symal_b)=("no","no","no");

  $__symal_d="yes" if $___ALIGNMENT=~ /diag/;
  $__symal_f="yes" if $___ALIGNMENT=~ /final/;
  $__symal_b="yes" if $___ALIGNMENT=~ /final-and/;
  
  safesystem("$GIZA2BAL -d $__ALIGNMENT_INV_CMD -i $__ALIGNMENT_CMD |".
			 "$SYMAL -alignment=\"$__symal_a\" -diagonal=\"$__symal_d\" ".
			 "-final=\"$__symal_f\" -both=\"$__symal_b\"".
			 " -o=$___ALIGNMENT_FILE.$___ALIGNMENT") 
      ||
	  die "ERROR: Can't generate symmetrized alignment file\n"
	  
}

sub run_single_giza {
  my($dir,$e,$f,$vcb_e,$vcb_f,$train) = @_;

  my %GizaDefaultOptions = 
	  (p0 => .999 ,
	   m1 => 5 , 
	   m2 => 0 , 
	   m3 => 3 , 
	   m4 => 3 , 
	   o => "giza" ,
	   nodumps => 1 ,
	   onlyaldumps => 1 ,
	   nsmooth => 4 , 
	   model1dumpfrequency => 1,
	   model4smoothfactor => 0.4 ,
	   t => $vcb_f,
	   s => $vcb_e,
	 c => $train,
	 CoocurrenceFile => "$dir/$f-$e.cooc",
	   o => "$dir/$f-$e");
	
	if (defined $_DICTIONARY)
	{ $GizaDefaultOptions{d} = $___CORPUS_DIR."/gizadict.$f-$e"; }
	
	# 5 Giza threads
	if (defined $_MGIZA){ $GizaDefaultOptions{"ncpus"} = $_MGIZA_CPUS; }

    if ($_HMM_ALIGN) {
       $GizaDefaultOptions{m3} = 0;
       $GizaDefaultOptions{m4} = 0;
       $GizaDefaultOptions{hmmiterations} = 5;
       $GizaDefaultOptions{hmmdumpfrequency} = 5;
       $GizaDefaultOptions{nodumps} = 0;
    }

    if ($___FINAL_ALIGNMENT_MODEL) {
        $GizaDefaultOptions{nodumps} =               ($___FINAL_ALIGNMENT_MODEL =~ /^[345]$/)? 1: 0;
	   $GizaDefaultOptions{model345dumpfrequency} = 0;
	   
	   $GizaDefaultOptions{model1dumpfrequency} =   ($___FINAL_ALIGNMENT_MODEL eq '1')? 5: 0;
	   
	   $GizaDefaultOptions{m2} =                    ($___FINAL_ALIGNMENT_MODEL eq '2')? 5: 0;
	   $GizaDefaultOptions{model2dumpfrequency} =   ($___FINAL_ALIGNMENT_MODEL eq '2')? 5: 0;
	   
	   $GizaDefaultOptions{hmmiterations} =         ($___FINAL_ALIGNMENT_MODEL =~ /^(hmm|[345])$/)? 5: 0;
	   $GizaDefaultOptions{hmmdumpfrequency} =      ($___FINAL_ALIGNMENT_MODEL eq 'hmm')? 5: 0;
	   
	   $GizaDefaultOptions{m3} =                    ($___FINAL_ALIGNMENT_MODEL =~ /^[345]$/)? 3: 0;
	   $GizaDefaultOptions{m4} =                    ($___FINAL_ALIGNMENT_MODEL =~ /^[45]$/)? 3: 0;
	   $GizaDefaultOptions{m5} =                    ($___FINAL_ALIGNMENT_MODEL eq '5')? 3: 0;
	  }

  if ($___GIZA_OPTION) {
	foreach (split(/[ ,]+/,$___GIZA_OPTION)) {
	  my ($option,$value) = split(/=/,$_,2);
	  $GizaDefaultOptions{$option} = $value;
	}
  }

  my $GizaOptions;
  foreach my $option (sort keys %GizaDefaultOptions){
	my $value = $GizaDefaultOptions{$option} ;
	$GizaOptions .= " -$option $value" ;
  }
  
  &run_single_snt2cooc($dir,$e,$f,$vcb_e,$vcb_f,$train) if $___PARTS == 1;

  print STDERR "(2.1b) running giza $f-$e @ ".`date`."$GIZA $GizaOptions\n";


  if (-e "$dir/$f-$e.$___GIZA_EXTENSION.gz") {
	print "  $dir/$f-$e.$___GIZA_EXTENSION.gz seems finished, reusing.\n";
	return;
  }
  print "$GIZA $GizaOptions\n";
  return if  $___ONLY_PRINT_GIZA;
  safesystem("$GIZA $GizaOptions");
  
  if (defined $_MGIZA and (!defined $___FINAL_ALIGNMENT_MODEL or $___FINAL_ALIGNMENT_MODEL ne '2')){
	print STDERR "Merging $___GIZA_EXTENSION.part\* tables\n";
	safesystem("$MGIZA_MERGE_ALIGN  $dir/$f-$e.$___GIZA_EXTENSION.part*>$dir/$f-$e.$___GIZA_EXTENSION");
	#system("rm -f $dir/$f-$e/*.part*");
  }


  die "ERROR: Giza did not produce the output file $dir/$f-$e.$___GIZA_EXTENSION. Is your corpus clean (reasonably-sized sentences)?"
      if ! -e "$dir/$f-$e.$___GIZA_EXTENSION";
  safesystem("rm -f $dir/$f-$e.$___GIZA_EXTENSION.gz") or die;
  safesystem("gzip $dir/$f-$e.$___GIZA_EXTENSION") or die;
}

sub make_classes {
  my ($corpus,$classes) = @_;
  my $cmd = "$MKCLS -c50 -n2 -p$corpus -V$classes opt";
  print STDERR "(1.1) running mkcls  @ ".`date`."$cmd\n";
  if (-e $classes) {
	print STDERR "  $classes already in place, reusing\n";
	return;
  }
  safesystem("$cmd"); # ignoring the wrong exit code from mkcls (not dying)
}

sub get_vocabulary {
  return unless $___LEXICAL_WEIGHTING;
  my($corpus,$vcb) = @_;
  print STDERR "(1.2) creating vcb file $vcb @ ".`date`;
  
  my %WORD;
  open(TXT,$corpus) or die "ERROR: Can't read $corpus";
  while(<TXT>) {
	chop;
	foreach (split) { $WORD{$_}++; }
  }
  close(TXT);
  
  my @NUM;
  foreach my $word (keys %WORD) {
	my $vcb_with_number = sprintf("%07d %s",$WORD{$word},$word);
	push @NUM,$vcb_with_number;
  }
  
  my %VCB;
  open(VCB,">$vcb") or die "ERROR: Can't write $vcb";
  print VCB "1\tUNK\t0\n";
  my $id=2;
  foreach (reverse sort @NUM) {
	my($count,$word) = split;
	printf VCB "%d\t%s\t%d\n",$id,$word,$count;
	$VCB{$word} = $id;
	$id++;
  }
  close(VCB);
  
  return \%VCB;
}

sub run_single_snt2cooc {
  my($dir,$e,$f,$vcb_e,$vcb_f,$train) = @_;
  print STDERR "(2.1a) running snt2cooc $f-$e @ ".`date`."\n";
  safesystem("mkdir -p $dir") or die("ERROR");
  print "$SNT2COOC $vcb_e $vcb_f $train > $dir/$f-$e.cooc\n";
  safesystem("$SNT2COOC $vcb_e $vcb_f $train > $dir/$f-$e.cooc") or die("ERROR");
}

sub safesystem {
  unshift (@_, '-c');
  unshift (@_, 'bash');
  print STDERR "Executing: @_\n";
  system(@_);
  if ($? == -1) {
	print STDERR "ERROR: Failed to execute: @_\n  $!\n";
	exit(1);
  }
  elsif ($? & 127) {
	printf STDERR "ERROR: Execution of: @_\n  died with signal %d, %s coredump\n",
	($? & 127),  ($? & 128) ? 'with' : 'without';
	exit(1);
  }
  else {
    my $exitcode = $? >> 8;
    print STDERR "Exit code: $exitcode\n" if $exitcode;
    return ! $exitcode;
  }
}

sub numberize_txt_file {
  my ($VCB_DE,$in_de,$VCB_EN,$in_en,$out) = @_;
  my %OUT;
  print STDERR "(1.3) numberizing corpus $out @ ".`date`;
  if (-e $out) {
	print STDERR "  $out already in place, reusing\n";
	return;
  }
  open(IN_DE,$in_de) or die "ERROR: Can't read $in_de";
  open(IN_EN,$in_en) or die "ERROR: Can't read $in_en";
  open(OUT,">$out") or die "ERROR: Can't write $out";
  while(my $de = <IN_DE>) {
	my $en = <IN_EN>;
	print OUT "1\n";
	print OUT &numberize_line($VCB_EN,$en);
	print OUT &numberize_line($VCB_DE,$de);
  }
  close(IN_DE);
  close(IN_EN);
  close(OUT);
}

sub numberize_line {
  my ($VCB,$txt) = @_;
  chomp($txt);
  my $out = "";
  my $not_first = 0;
  foreach (split(/ /,$txt)) { 
	next if $_ eq '';
	$out .= " " if $not_first++;
	print STDERR "Unknown word '$_'\n" unless defined($$VCB{$_});
	$out .= $$VCB{$_};
  }
  return $out."\n";
}
