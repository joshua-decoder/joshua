#!/usr/bin/perl

# This script implements the Joshua pipeline.  It can run a complete
# pipeline --- from raw training corpora to bleu scores on a test set
# --- and it allows jumping into arbitrary points of the pipeline.  It
# is modeled on the train-factored-phrase-model.perl from the Moses
# decoder, but it is built for hierarchical decoding, and handles
# parameter tuning (via MERT) and test-set decoding, as well.
#
# Currently implemented:
#
# - decoding with Hiero grammars
# - jump to GIZA, MERT, THRAX, and TEST points (using --first-step and
#   (optionally) --last-step)
# - built on top of CachePipe, so that intermediate results are cached
#   and only re-run if necessary
# - uses Thrax for grammar extraction
#
# Imminent:
#
# - support for subsampling the training corpus to a development set
# - support for SAMT grammars

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use Cwd;
use CachePipe;

my $HADOOP = $ENV{HADOOP};
my $JOSHUA = $ENV{JOSHUA};
my $THRAX  = $ENV{THRAX};

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$FR,$EN,$LMFILE,$GRAMMAR_FILE);
my $FIRST_STEP = "FIRST";
my $LAST_STEP  = "LAST";
my $LMFILTER = "$ENV{HOME}/code/filter/filter";
my $MAXLEN = 50;
my $DO_FILTER_LM = 1;
my $DO_SUBSAMPLE = ''; # default false
my $SCRIPTDIR = "$JOSHUA/scripts";
my $TOKENIZER = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $MOSES_TRAINER = "/home/hltcoe/airvine/bin/moses/tools/moses-scripts/scripts-20100922-0942/training/train-factored-phrase-model.perl";
my $MERTCONFDIR = "$JOSHUA/scripts/training/templates/mert";
my $SRILM = "$ENV{SRILM}/bin/i686-m64/ngram-count";
my $STARTDIR;
my $RUNDIR = $STARTDIR = getcwd;
my $GRAMMAR_TYPE = "hiero";

# this file should exist in the Joshua mert templates file; it contains
# the Joshua command invoked by MERT
my $JOSHUA_CONFIG_ORIG   = "$MERTCONFDIR/joshua.config";
my %MERTFILES = (
  'decoder_command' => "$MERTCONFDIR/decoder_command.qsub",
  'joshua.config'   => $JOSHUA_CONFIG_ORIG,
  'params.txt'      => "$MERTCONFDIR/params.txt",
  'mert.config'     => "$MERTCONFDIR/mert.config"
);

my $DO_MBR = 1;

# for hadoop java subprocesses (heap amount)
# you really just have to play around to find out how much is enough 
my $HADOOP_MEM = "8G";  
my $JOSHUA_MEM = "3100m";
my $QSUB_ARGS  = "-l num_proc=2";

my %STEPS = (
  FIRST => 1,
  GIZA => 2,
  THRAX => 3,
  MERT => 4,
  TEST => 5,
  LAST => 6,
);

my $options = GetOptions(
  "corpus=s" 	 	  => \@CORPORA,
  "tune=s"   	 	  => \$TUNE,
  "test=s"            => \$TEST,
  "alignment=s"  	  => \$ALIGNMENT,
  "fr=s"     	 	  => \$FR,
  "en=s"     	 	  => \$EN,
  "rundir=s" 	 	  => \$RUNDIR,
  "filter-lm!"        => \$DO_FILTER_LM,
  "lmfile=s" 	 	  => \$LMFILE,
  "grammar=s"    	  => \$GRAMMAR_FILE,
  "mbr!"              => \$DO_MBR,
  "type=s"       	  => \$GRAMMAR_TYPE,
  "maxlen=i" 	 	  => \$MAXLEN,
  "tokenizer=s"  	  => \$TOKENIZER,
  "joshua-config=s"   => \$MERTFILES{'joshua.config'},
  "decoder-command=s" => \$MERTFILES{'decoder_command'},
  "subsample!"   	  => \$DO_SUBSAMPLE,
  "qsub-args=s"  	  => \$QSUB_ARGS,
  "first-step=s" 	  => \$FIRST_STEP,
  "last-step=s"  	  => \$LAST_STEP,
);

$| = 1;

my $cachepipe = new CachePipe();

## Sanity Checking ###################################################

if (! defined $TUNE and ($STEPS{$FIRST_STEP} <= $STEPS{MERT}
						 and $STEPS{$LAST_STEP} >= $STEPS{MERT})) { 
  print "* FATAL: need a tuning set (--tune)\n";
  exit 1;
}

if (! defined $TEST and ($STEPS{$FIRST_STEP} <= $STEPS{TEST}
						 and $STEPS{$LAST_STEP} >= $STEPS{TEST})) {
  print "* FATAL: need a test set (--test)\n";
  exit 1;
}

# if $CORPUS was a relative path, prepend the starting directory
# (under the assumption it was relative to there)
map {
  $CORPORA[$_] = "$STARTDIR/$CORPORA[$_]" unless $CORPORA[$_] =~ /^\//;
} (0..$#CORPORA);

foreach my $corpus (@CORPORA) {
  foreach my $ext ($EN,$FR) {
	if (! -e "$corpus.$ext") {
	  print "* FATAL: can't find '$corpus.$ext'";
	  exit 1;
	}
  }
}

## Dependent variable setting ########################################

my $OOV = ($GRAMMAR_TYPE eq "samt") ? "OOV" : "X";
my $THRAXDIR = "pipeline-$FR-$EN-$GRAMMAR_TYPE";

mkdir $RUNDIR unless -d $RUNDIR;
chdir($RUNDIR);

# default values -- these are overridden if the full script is run
# (after tokenization and normalization)
my (%TRAIN,%TUNE,%TEST);
if (@CORPORA) {
  $TRAIN{prefix} = $CORPORA[0];
  $TRAIN{fr} = "$CORPORA[0].$FR";
  $TRAIN{en} = "$CORPORA[0].$EN";
}

$TUNE{fr} = "$TUNE.$FR";
$TUNE{en} = "$TUNE.$EN";

if ($TEST) {
  $TEST{fr} = "$TEST.$FR";
  $TEST{en} = "$TEST.$EN";
}

if ($FIRST_STEP ne "FIRST") {
  if (@CORPORA > 1) {
	print "* FATAL: you can't skip steps if you specify more than one --corpus\n";
	exit(1);
  }

  if (eval { goto $FIRST_STEP }) {
	print "* Skipping to step $FIRST_STEP\n";
	goto $FIRST_STEP;
  } else {
	print "* No such step $FIRST_STEP\n";
	exit 1;
  }
}

## STEP 1: filter and preprocess corpora #############################
FIRST:

if (@CORPORA == 0) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

# prepare the training data
prepare_data("train",\@CORPORA,$MAXLEN);
$TRAIN{prefix} = "train/corpus";
$TRAIN{fr} = "train/corpus.$FR";
$TRAIN{en} = "train/corpus.$EN";

# prepare the tuning and development data
prepare_data("tune",[$TUNE]);
$TUNE{fr} = "tune/tune.tok.lc.$FR";
$TUNE{en} = "tune/tune.tok.lc.$EN";

prepare_data("test",[$TEST]);
$TEST{fr} = "test/test.tok.lc.$FR";
$TEST{en} = "test/test.tok.lc.$EN";

# subsample
if ($DO_SUBSAMPLE) {
  print "\n\n** DO_SUBSAMPLE UNIMPLEMENTED\n\n";
  exit 1;

  mkdir("train/subsampled");

  $cachepipe->cmd("subsample-manifest",
				  "echo train/corpus > train/subsampled/manifest",
				  "train/subsampled/manifest");

  $cachepipe->cmd("subsample-testdata",
				  "cat $TUNE.$FR $TEST.$FR > train/subsampled/test-data",
				  "$TUNE.$FR", "$TEST.$FR", "train/subsampled/test-data");

  $cachepipe->cmd("subsample",
				  "java -Xmx4g -Dfile.encoding=utf8 -cp $JOSHUA/bin:$JOSHUA/lib/commons-cli-2.0-SNAPSHOT.jar joshua.subsample.Subsampler -e $EN.tok.$MAXLEN -f $FR.tok.$MAXLEN -epath train/ -fpath train/ -output train/subsampled/subsampled.$MAXLEN -ratio 1.04 -test train/subsampled/test-data -training train/subsampled/manifest",
				  "train/subsampled/manifest",
				  "train/subsampled/test-data",
				  "train/corpus.$EN.tok.$MAXLEN",
				  "train/corpus.$FR.tok.$MAXLEN",
				  "train/subsampled/subsampled.$MAXLEN.$EN.tok.$MAXLEN",
				  "train/subsampled/subsampled.$MAXLEN.$FR.tok.$MAXLEN");

  foreach my $lang ($EN,$FR) {
	$cachepipe->cmd("link-corpus",
					"ln -sf subsampled/subsampled.$MAXLEN.$lang.tok.$MAXLEN train/corpus.$lang",
					"train/corpus.$lang");
  }
} else {
  foreach my $lang ($EN,$FR) {
	$cachepipe->cmd("link-corpus-$lang",
					"ln -sf train.tok.$MAXLEN.lc.$lang train/corpus.$lang",
					"train/corpus.$lang");
  }
}

## GIZA ##############################################################

GIZA:

# alignment
$ALIGNMENT = "giza/model/aligned.grow-diag-final";
$cachepipe->cmd("giza",
				"rm -f train/corpus.0-0.*; $MOSES_TRAINER -root-dir giza -e $EN -f $FR -corpus $TRAIN{prefix} -first-step 1 -last-step 3 > giza.log 2>&1",
				"$TRAIN{prefix}.$FR",
				"$TRAIN{prefix}.$EN",
				$ALIGNMENT);


if ($GRAMMAR_TYPE eq "samt") {

#   cachecmd parse "~mpost/code/cdec/vest/parallelize.pl -j 20 -- java -cp /home/hltcoe/mpost/code/berkeleyParser edu.berkeley.nlp.PCFGLA.BerkeleyParser -gr /home/hltcoe/mpost/code/berkeleyParser/eng_sm5.gr <subsampled/subsample.$pair.$maxlen.$en.tok | sed s/^\(/\(TOP/ >subsampled/subsample.$pair.$maxlen.$en.parsed" subsampled/subsample.$pair.$maxlen.$en.{tok,parsed}

  $TRAIN{EN} = "$TRAIN{prefix}.$EN.parsed";
}

maybe_quit("GIZA");

## THRAX #############################################################

THRAX:

# we may have skipped directly to this step, in which case we need to
# ensure an alignment was provided
if (! defined $ALIGNMENT) {
  print "* FATAL: no alignment file specified\n";
  exit(1);
}

if (! defined $GRAMMAR_FILE) {
  mkdir("train") unless -d "train";

# create the input file
  $cachepipe->cmd("thrax-input-file",
				  "paste $TRAIN{fr} $TRAIN{en} $ALIGNMENT | perl -pe 's/\t/ ||| /g' | grep -v '(())' > train/thrax-input-file",
				  $TRAIN{fr}, $TRAIN{en}, $ALIGNMENT,
				  "train/thrax-input-file");

# put the hadoop files in place
  $cachepipe->cmd("thrax-prep",
				  "$HADOOP/bin/hadoop fs -rmr $THRAXDIR; $HADOOP/bin/hadoop fs -mkdir $THRAXDIR; $HADOOP/bin/hadoop fs -put train/thrax-input-file $THRAXDIR/input-file",
				  "train/thrax-input-file", 
				  "grammar.gz");

  copy_thrax_file();

  $cachepipe->cmd("thrax-run",
				  "$HADOOP/bin/hadoop jar $THRAX/bin/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' thrax-$GRAMMAR_TYPE.conf $THRAXDIR > thrax.log 2>&1; rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $THRAXDIR/final/ grammar; gzip -9 grammar",
				  "train/thrax-input-file",
				  "grammar.gz");

  $GRAMMAR_FILE = "grammar.gz";
}

maybe_quit("THRAX");

## MERT ##############################################################
MERT:

# maybe_copy_tuning_data();

# If the language model file wasn't provided, build it from the target side of the training data.  Otherwise, copy it to location.
if (! defined $LMFILE) {
  if (exists $TRAIN{en}) {
	$LMFILE="lm.gz";
	$cachepipe->cmd("srilm",
					"$SRILM -interpolate -kndiscount -order 5 -text $TRAIN{en} -lm lm.gz",
					$LMFILE);
  } elsif (! defined $LMFILE) {
	print "* FATAL: you skipped training and didn't specify a language model\n";
	exit(1);
  }
} else {
  if (! -e $LMFILE) {
	print STDERR "* FATAL: can't file lmfile '$LMFILE'\n";
	exit(1);
  }

  if ($LMFILE ne "lm.gz") {
	$cachepipe->cmd("cp-lmfile",
					"cp $LMFILE lm.gz",
					$LMFILE, "lm.gz");
	$LMFILE = "lm.gz";
  }
}

# filter the tuning LM to the training side of the data (if possible)
if (-e $LMFILTER and $DO_FILTER_LM and exists $TRAIN{en}) {
  $cachepipe->cmd("filter-lmfile",
				  "$LMFILTER union arpa model:$LMFILE lm-filtered < $TRAIN{en}; gzip -9f lm-filtered",
				  $LMFILE, "lm-filtered.gz");
  $LMFILE = "lm-filtered.gz";
}

mkdir("tune") unless -d "tune";

# filter the tuning grammar
$cachepipe->cmd("filter-tune",
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | $THRAX/scripts/filter_rules.sh -v $TUNE{fr} | gzip -9 > tune/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TUNE{fr},
				"tune/grammar.filtered.gz");

copy_thrax_file();
$cachepipe->cmd("glue-tune",
				"$SCRIPTDIR/training/scat tune/grammar.filtered.gz | $THRAX/scripts/create_glue_grammar.sh thrax-$GRAMMAR_TYPE.conf > tune/grammar.glue",
				"tune/grammar.filtered.gz",
				"tune/grammar.glue");

# figure out how many references there are
my $numrefs = 1;
if (! -e $TUNE{en}) {
  my $index = 0;
  while (-e "$TUNE{en}.$index") {
	$index++;
  }
  $numrefs = $index;
}

mkdir("mert") unless -d "mert";
foreach my $key (keys %MERTFILES) {
  my $file = $MERTFILES{$key};
  open FROM, $file or die "can't find file '$file'";
  open TO, ">mert/$key" or die "can't write to file 'mert/$key'";
  while (<FROM>) {
	s/<INPUT>/$TUNE{fr}/g;
	s/<FR>/$FR/g;
	s/<EN>/$EN/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR_TYPE/g;
	s/<OOV>/$OOV/g;
	s/<NUMJOBS>/50/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/mert\/tune.output.nbest/g;
	s/<REF>/$TUNE{en}/g;
	s/<NUMREFS>/$numrefs/g;
	s/<CONFIG>/test\/joshua.config/g;
	s/<LOG>/test\/mert.log/g;

	print TO;
  }
  close(FROM);
  close(TO);
}
chmod(0755,"mert/decoder_command");

# run MERT
$cachepipe->cmd("mert",
				"java -d64 -cp $JOSHUA/bin joshua.zmert.ZMERT -maxMem 4500 mert/mert.config > mert/mert.log 2>&1",
				"tune/grammar.filtered.gz",
				"mert/joshua.config.ZMERT.final",
				map { "mert/$_" } (keys %MERTFILES));

# remove sentence-level Joshua files
#system("rm -rf tune/filtered/");

maybe_quit("MERT");

# set joshua config file location for testing
# $JOSHUA_CONFIG = "mert/joshua.config.ZMERT.final";

# copy the config file for testing (since we know we're not quitting
# at the MERT step)
if ($LAST_STEP ne "MERT") {
  $cachepipe->cmd("test-joshua-config",
				  "cat mert/joshua.config | perl -pe 's#tune/#test/#; s/mark_oovs=false/mark_oovs=true/; s/keep_sent_specific_tm=true/keep_sent_specific_tm=false/' > test/joshua.config",
				  $MERTFILES{'joshua.config'},
				  "test/joshua.config");
}

## Decode the test set
TEST:

mkdir("test") unless -d "test";

## sanity checking
# if we jumped in here, make sure a joshua.config file was specified
if ($FIRST_STEP eq "TEST") {
  if ($MERTFILES{'joshua.config'} eq $JOSHUA_CONFIG_ORIG) {
	print "* FATAL: you need to explicitly specify a joshua.config (--joshua-config)\n";
	print "         when starting at the TEST step\n";
	exit 1;
  }

  $cachepipe->cmd("test-joshua-config",
				  "cp $MERTFILES{'joshua.config'} test/joshua.config",
				  $MERTFILES{'joshua.config'},
				  "test/joshua.config");
}

# filter the test grammar
$cachepipe->cmd("filter-test",
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | $THRAX/scripts/filter_rules.sh -v $TEST{fr} | gzip -9 > test/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TEST{fr},
				"test/grammar.filtered.gz");

copy_thrax_file();
$cachepipe->cmd("glue-test",
				"$SCRIPTDIR/training/scat test/grammar.filtered.gz | $THRAX/scripts/create_glue_grammar.sh thrax-$GRAMMAR_TYPE.conf > test/grammar.glue",
				"test/grammar.filtered.gz",
				"test/grammar.glue");

# decode test set
foreach my $key (qw(decoder_command)) {
  my $file = $MERTFILES{$key};
  open FROM, $file or die "can't find file '$file'";
  open TO, ">test/$key" or die "can't write to 'test/$key'";
  while (<FROM>) {
	s/<INPUT>/$TEST{fr}/g;
	s/<NUMJOBS>/50/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/test\/test.output.nbest/g;
	s/<NUMREFS>/$numrefs/g;
	s/<FR>/$FR/g;
	s/<EN>/$EN/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR_TYPE/g;
	s/<OOV>/$OOV/g;
	s/<CONFIG>/test\/joshua.config/g;
	s/<LOG>/test\/mert.log/g;

	print TO;
  }
  close(FROM);
  close(TO);
}
chmod(0755,"test/decoder_command");

$cachepipe->cmd("test-decode",
				"./test/decoder_command",
				"test/decoder_command",
				"test/grammar.glue",
				"test/grammar.filtered.gz",
				"test/test.output.nbest");

if ($DO_MBR) {
  $cachepipe->cmd("test-onebest-mbr", "java -cp $JOSHUA/bin -Xmx1700m -Xms1700m joshua.decoder.NbestMinRiskReranker test/test.output.nbest test/test.output.1best false 1",
				  "test/test.output.nbest", "test/test.output.1best");
} else {
  $cachepipe->cmd("test-extract-onebest",
				  "java -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.util.ExtractTopCand test/test.output.nbest test/test.output.1best",
				  "test/test.output.nbest", "test/test.output.1best");
}

$cachepipe->cmd("test-bleu",
				"java -cp $JOSHUA/bin -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand test/test.output.1best -ref $TEST{en} -m BLEU 4 closest > test/test.output.1best.bleu",
				"test/test.output.1best", "test/test.output.1best.bleu");

system("cat test/test.output.1best.bleu");
				
######################################################################
## SUBROUTINES #######################################################
######################################################################
LAST:
1;

sub copy_thrax_file {
  $cachepipe->cmd("thrax-config",
				  "cp $JOSHUA/scripts/training/templates/thrax-$GRAMMAR_TYPE.conf .; echo input-file $THRAXDIR/input-file >> thrax-$GRAMMAR_TYPE.conf",
				  "$JOSHUA/scripts/training/templates/thrax-$GRAMMAR_TYPE.conf",
				  "thrax-$GRAMMAR_TYPE.conf");
}

# applies if the early steps were skipped
# sub maybe_copy_tuning_data {
#   if (! -e "tune/tune.$FR.tok.lc") {
# 	$cachepipe->cmd("cp-tuning-data",
# 					"cp $TUNE{fr} tune/tune.$FR.tok.lc; cp $TUNE{en} tune/tune.$EN.tok.lc",
# 					$TUNE{fr}, "tune/tune.$FR.tok.lc",
# 					$TUNE{en}, "tune/tune.$EN.tok.lc");
# 	$TUNE{fr} = "tune/tune.$FR.tok.lc";
# 	$TUNE{en} = "tune/tune.$EN.tok.lc";
#   }
# }

# # copies test data over if early stages were skipped
# sub maybe_copy_test_data {
#   if (! -e "test/test.$FR.tok.lc") {
# 	$cachepipe->cmd("cp-test-data",
# 					"cp $TEST{fr} test/test.$FR.tok.lc; cp $TEST{en} test/test.$EN.tok.lc",
# 					$TEST{fr}, "test/test.$FR.tok.lc",
# 					$TEST{en}, "test/test.$EN.tok.lc");
# 	$TEST{fr} = "test/test.$FR.tok.lc";
# 	$TEST{en} = "test/test.$EN.tok.lc";
#   }
# }


sub prepare_data {
  my ($label,$corpora,$maxlen) = @_;

  mkdir $label unless -d $label;

  # copy the data from its original location to our location
  foreach my $lang ($EN,$FR,"$EN.0","$EN.1","$EN.2","$EN.3") {
	  my @files = map { "$_.$lang" } @$corpora;
	  my $files = join(" ",@files);
	  if (-e $files[0]) {
		$cachepipe->cmd("$label-copy-$lang",
						"cat $files > $label/$label.$lang",
						@files, "$label/$label.$lang");
	  }
  }

  # tokenize the data
  foreach my $lang ($EN,$FR,"$EN.0","$EN.1","$EN.2","$EN.3") {
	if (-e "$label/$label.$lang") {
	  $cachepipe->cmd("$label-tokenize-$lang",
					  "$SCRIPTDIR/training/scat $label/$label.$lang | $TOKENIZER -l $lang > $label/$label.tok.$lang 2> /dev/null",
					  "$label/$label.$lang", "$label/$label.tok.$lang"
		  );
	}
  }

  my $infix = "";
  if ($maxlen and $label eq "train") {
	# trim training data
	$cachepipe->cmd("train-trim",
					"$SCRIPTDIR/training/trim_parallel_corpus.pl $label/$label.tok.$EN $label/$label.tok.$FR $maxlen > $label/$label.tok.$maxlen.$EN 2> $label/$label.tok.$maxlen.$FR",
					"$label/$label.tok.$EN", "$label/$label.tok.$FR",
					"$label/$label.tok.$maxlen.$EN", "$label/$label.tok.$maxlen.$FR",
		);
	$infix = ".$maxlen";
  }

  # lowercase
  foreach my $lang ($EN,$FR,"$EN.0","$EN.1","$EN.2","$EN.3") {
	if (-e "$label/$label.$lang") {
	  $cachepipe->cmd("$label-lowercase-$lang",
					  "cat $label/$label.tok$infix.$lang | $SCRIPTDIR/lowercase.perl > $label/$label.tok$infix.lc.$lang",
					  "$label/$label.tok$infix.$lang",
					  "$label/$label.tok$infix.lc.$lang");
	}
  }
}

sub maybe_quit {
  my ($current_step) = @_;

  if (defined $LAST_STEP and $current_step eq $LAST_STEP) {
	print "* Quitting at this step\n";
	exit(0);
  }
}

