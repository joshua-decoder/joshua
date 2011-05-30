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
# - jump to GIZA, PARSE, MERT, THRAX, and TEST points (using --first-step and
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

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$SOURCE,$TARGET,$LMFILE,$GRAMMAR_FILE,$THRAX_CONF_FILE);
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
  'mert.config'     => "$MERTCONFDIR/mert.config",
  'params.txt'      => "$MERTCONFDIR/params.txt",
);

my $DO_SENT_SPECIFIC_TM = 1;
my $DO_MBR = 1;

# for hadoop java subprocesses (heap amount)
# you really just have to play around to find out how much is enough 
my $HADOOP_MEM = "8G";  
my $JOSHUA_MEM = "3100m";
my $QSUB_ARGS  = "-l num_proc=2";

my @STEPS = qw[FIRST ALIGN THRAX MERT TEST LAST];
my %STEPS = map { $STEPS[$_] => $_ + 1 } (0..$#STEPS);

my $retval = GetOptions(
  "corpus=s" 	 	  => \@CORPORA,
  "tune=s"   	 	  => \$TUNE,
  "test=s"            => \$TEST,
  "alignment=s"  	  => \$ALIGNMENT,
  "source=s"   	 	  => \$SOURCE,
  "target=s"  	 	  => \$TARGET,
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
  "thrax-conf=s"      => \$THRAX_CONF_FILE,
  "subsample!"   	  => \$DO_SUBSAMPLE,
  "qsub-args=s"  	  => \$QSUB_ARGS,
  "first-step=s" 	  => \$FIRST_STEP,
  "last-step=s"  	  => \$LAST_STEP,
  "sentence-tm!"      => \$DO_SENT_SPECIFIC_TM,
);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

$| = 1;

my $cachepipe = new CachePipe();

$SIG{INT} = sub { 
  print "* Got C-c, quitting\n";
  $cachepipe->cleanup();
  exit 1; 
};

## Sanity Checking ###################################################

# make sure a corpus was provided if we're doing any step before MERT
if (@CORPORA == 0 and $STEPS{$FIRST_STEP} < $STEPS{MERT}) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

# make sure a tuning corpus was provided if we're doing MERT
if (! defined $TUNE and ($STEPS{$FIRST_STEP} <= $STEPS{MERT}
						 and $STEPS{$LAST_STEP} >= $STEPS{MERT})) { 
  print "* FATAL: need a tuning set (--tune)\n";
  exit 1;
}

# make sure a test corpus was provided if we're decoding a test set
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
  foreach my $ext ($TARGET,$SOURCE) {
	if (! -e "$corpus.$ext") {
	  print "* FATAL: can't find '$corpus.$ext'";
	  exit 1;
	}
  }
}

## Dependent variable setting ########################################

my $OOV = ($GRAMMAR_TYPE eq "samt") ? "OOV" : "X";
my $THRAXDIR = "pipeline-$SOURCE-$TARGET-$GRAMMAR_TYPE";

# use this default unless it's already been defined by a command-line argument
$THRAX_CONF_FILE = "$JOSHUA/scripts/training/templates/thrax-$GRAMMAR_TYPE.conf" unless defined $THRAX_CONF_FILE;

mkdir $RUNDIR unless -d $RUNDIR;
chdir($RUNDIR);

# default values -- these are overridden if the full script is run
# (after tokenization and normalization)
my (%TRAIN,%TUNE,%TEST);
if (@CORPORA) {
  $TRAIN{prefix} = $CORPORA[0];
  $TRAIN{source} = "$CORPORA[0].$SOURCE";
  $TRAIN{target} = "$CORPORA[0].$TARGET";
}

$TUNE{source} = "$TUNE.$SOURCE";
$TUNE{target} = "$TUNE.$TARGET";

if ($TEST) {
  $TEST{source} = "$TEST.$SOURCE";
  $TEST{target} = "$TEST.$TARGET";
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
$TRAIN{source} = "train/corpus.$SOURCE";
$TRAIN{target} = "train/corpus.$TARGET";

# prepare the tuning and development data
prepare_data("tune",[$TUNE]);
$TUNE{source} = "tune/tune.tok.lc.$SOURCE";
$TUNE{target} = "tune/tune.tok.lc.$TARGET";

prepare_data("test",[$TEST]);
$TEST{source} = "test/test.tok.lc.$SOURCE";
$TEST{target} = "test/test.tok.lc.$TARGET";

# subsample
if ($DO_SUBSAMPLE) {
  print "\n\n** DO_SUBSAMPLE UNIMPLEMENTED\n\n";
  exit 1;

  mkdir("train/subsampled");

  $cachepipe->cmd("subsample-manifest",
				  "echo train/corpus > train/subsampled/manifest",
				  "train/subsampled/manifest");

  $cachepipe->cmd("subsample-testdata",
				  "cat $TUNE.$SOURCE $TEST.$SOURCE > train/subsampled/test-data",
				  "$TUNE.$SOURCE", "$TEST.$SOURCE", "train/subsampled/test-data");

  $cachepipe->cmd("subsample",
				  "java -Xmx4g -Dfile.encoding=utf8 -cp $JOSHUA/bin:$JOSHUA/lib/commons-cli-2.0-SNAPSHOT.jar joshua.subsample.Subsampler -e $TARGET.tok.$MAXLEN -f $SOURCE.tok.$MAXLEN -epath train/ -fpath train/ -output train/subsampled/subsampled.$MAXLEN -ratio 1.04 -test train/subsampled/test-data -training train/subsampled/manifest",
				  "train/subsampled/manifest",
				  "train/subsampled/test-data",
				  "train/corpus.$TARGET.tok.$MAXLEN",
				  "train/corpus.$SOURCE.tok.$MAXLEN",
				  "train/subsampled/subsampled.$MAXLEN.$TARGET.tok.$MAXLEN",
				  "train/subsampled/subsampled.$MAXLEN.$SOURCE.tok.$MAXLEN");

  foreach my $lang ($TARGET,$SOURCE) {
	$cachepipe->cmd("link-corpus-$lang",
					"ln -sf subsampled/subsampled.tok.$MAXLEN.lc.$lang train/corpus.$lang",
					"train/corpus.$lang");
	$cachepipe->cmd("link-corpus-tok-$lang",
					"ln -sf subsampled/subsampled.tok.$MAXLEN.lc.$lang train/corpus.tok.$lang",
					"train/corpus.tok.$lang");

  }
} else {
  foreach my $lang ($TARGET,$SOURCE) {
	$cachepipe->cmd("link-corpus-$lang",
					"ln -sf train.tok.$MAXLEN.lc.$lang train/corpus.$lang",
					"train/corpus.$lang");
	$cachepipe->cmd("link-corpus-tok-$lang",
					"ln -sf train.tok.$MAXLEN.$lang train/corpus.tok.$lang",
					"train/corpus.tok.$lang");

  }
}

## ALIGN #############################################################

ALIGN:

# alignment
$ALIGNMENT = "giza/model/aligned.grow-diag-final";
$cachepipe->cmd("giza",
				"rm -f train/corpus.0-0.*; $MOSES_TRAINER -root-dir giza -e $TARGET -f $SOURCE -corpus $TRAIN{prefix} -first-step 1 -last-step 3 > giza.log 2>&1",
				$TRAIN{source},
				$TRAIN{target},
				$ALIGNMENT);

maybe_quit("ALIGN");

## THRAX #############################################################

THRAX:

if ($GRAMMAR_TYPE eq "samt") {

  # check whether we need to parse (might be already parsed if we
  # jumped directly here)
  if (already_parsed($TRAIN{target})) {
	mkdir("train") unless -d "train";

	# If parsing was already done, then copy the file to our training
	# directory so we can use our local copy instead of the original
	# one.  Thrax later in the script expects to find
	# $TRAIN{target}.parsed.OOV, so that's where we copy it.  An important
	# note here is that to get this, we set $TRAIN{target} to point to a
	# file that doesn't exist.  This is okay because after the Thrax
	# run we should never need to use the training data any more (and
	# anyway we don't have it).  If that changes this will break
	# things.

	$cachepipe->cmd("cp-train-$TARGET",
					"cp $TRAIN{target} train/corpus.$TARGET.parsed.OOV",
					$TRAIN{target}, "train/corpus.$TARGET.parsed.OOV");
	$TRAIN{target} = "train/corpus.$TARGET";

	$cachepipe->cmd("cp-train-$SOURCE",
					"cp $TRAIN{source} train/corpus.$SOURCE",
					$TRAIN{source}, "train/corpus.$SOURCE");
	$TRAIN{source} = "train/corpus.$SOURCE";

  } else {

	$cachepipe->cmd("build-vocab",
					"cat $TRAIN{target} | $SCRIPTDIR/training/build-vocab.pl > train/vocab.$TARGET",
					$TRAIN{target},
					"train/vocab.$TARGET");

	$cachepipe->cmd("parse",
					"cat $TRAIN{prefix}.tok.$TARGET | /home/hltcoe/mpost/code/cdec/vest/parallelize.pl -j 50 -- java -cp /home/hltcoe/mpost/code/berkeleyParser edu.berkeley.nlp.PCFGLA.BerkeleyParser -gr /home/hltcoe/mpost/code/berkeleyParser/eng_sm5.gr | sed 's/^\(/\(TOP/' | tee $TRAIN{prefix}.$TARGET.parsed.mc | perl -pi -e 's/(\\S+)\\)/lc(\$1).\")\"/ge' | tee $TRAIN{prefix}.$TARGET.parsed | perl $SCRIPTDIR/training/add-OOVS.pl train/vocab.$TARGET > $TRAIN{prefix}.$TARGET.parsed.OOV",
					"$TRAIN{target}",
					"$TRAIN{target}.parsed.OOV");
  }
}

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
				  "paste $TRAIN{source} $TRAIN{target}.parsed.OOV $ALIGNMENT | perl -pe 's/\t/ ||| /g' | grep -v '(())' > train/thrax-input-file",
				  $TRAIN{source}, "$TRAIN{target}.parsed.OOV", $ALIGNMENT,
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

  # cache the thrax-prep step, which depends on grammar.gz
  $cachepipe->cmd("thrax-prep", "--cache-only");

  # set the grammar file
  $GRAMMAR_FILE = "grammar.gz";
}

maybe_quit("THRAX");

## MERT ##############################################################
MERT:

# If the language model file wasn't provided, build it from the target side of the training data.  Otherwise, copy it to location.
if (! defined $LMFILE) {
  if (exists $TRAIN{target}) {
	$LMFILE="lm.gz";
	$cachepipe->cmd("srilm",
					"$SRILM -interpolate -kndiscount -order 5 -text $TRAIN{target} -lm lm.gz",
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
if (-e $LMFILTER and $DO_FILTER_LM and exists $TRAIN{target}) {
  
  $cachepipe->cmd("filter-lmfile",
				  "cat $TRAIN{target} | $LMFILTER union arpa model:$LMFILE lm-filtered; gzip -9f lm-filtered",
				  $LMFILE, "lm-filtered.gz");
  $LMFILE = "lm-filtered.gz";
}

mkdir("tune") unless -d "tune";

# filter the tuning grammar
$cachepipe->cmd("filter-tune",
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | $THRAX/scripts/filter_rules.sh -v $TUNE{source} | gzip -9 > tune/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TUNE{source},
				"tune/grammar.filtered.gz");

copy_thrax_file();
$cachepipe->cmd("glue-tune",
				"$SCRIPTDIR/training/scat tune/grammar.filtered.gz | $THRAX/scripts/create_glue_grammar.sh thrax-$GRAMMAR_TYPE.conf > tune/grammar.glue",
				"tune/grammar.filtered.gz",
				"tune/grammar.glue");

# figure out how many references there are
my $numrefs = 1;
if (! -e $TUNE{target}) {
  my $index = 0;
  while (-e "$TUNE{target}.$index") {
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
	s/<INPUT>/$TUNE{source}/g;
	s/<SOURCE>/$SOURCE/g;
	s/$TARGET/$TARGET/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR_TYPE/g;
	s/<OOV>/$OOV/g;
	s/<NUMJOBS>/50/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/mert\/tune.output.nbest/g;
	s/<REF>/$TUNE{target}/g;
	s/<JOSHUA>/$JOSHUA/g;
	s/<NUMREFS>/$numrefs/g;
	s/<CONFIG>/mert\/joshua.config/g;
	s/<LOG>/mert\/joshua.log/g;

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
				"mert/decoder_command",
				"mert/mert.config",
				"mert/params.txt");

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
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | $THRAX/scripts/filter_rules.sh -v $TEST{source} | gzip -9 > test/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TEST{source},
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
	s/<INPUT>/$TEST{source}/g;
	s/<NUMJOBS>/50/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/test\/test.output.nbest/g;
	s/<JOSHUA>/$JOSHUA/g;
	s/<NUMREFS>/$numrefs/g;
	s/<SOURCE>/$SOURCE/g;
	s/$TARGET/$TARGET/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR_TYPE/g;
	s/<OOV>/$OOV/g;
	s/<CONFIG>/test\/joshua.config/g;
	s/<LOG>/test\/joshua.log/g;

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
				"java -cp $JOSHUA/bin -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand test/test.output.1best -ref $TEST{target} -m BLEU 4 closest > test/test.output.1best.bleu",
				"test/test.output.1best", "test/test.output.1best.bleu");

system("cat test/test.output.1best.bleu");
				
######################################################################
## SUBROUTINES #######################################################
######################################################################
LAST:
1;

# I don't know why this is a function
sub copy_thrax_file {
  $cachepipe->cmd("thrax-config",
				  "grep -v input-file $THRAX_CONF_FILE > thrax-$GRAMMAR_TYPE.conf; echo input-file $THRAXDIR/input-file >> thrax-$GRAMMAR_TYPE.conf",
				  $THRAX_CONF_FILE,
				  "thrax-$GRAMMAR_TYPE.conf");
}

# Does tokenization and normalization of training, tuning, and test data.
sub prepare_data {
  my ($label,$corpora,$maxlen) = @_;

  mkdir $label unless -d $label;

  # copy the data from its original location to our location
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
	  my @files = map { "$_.$lang" } @$corpora;
	  my $files = join(" ",@files);
	  if (-e $files[0]) {
		$cachepipe->cmd("$label-copy-$lang",
						"cat $files > $label/$label.$lang",
						@files, "$label/$label.$lang");
	  }
  }

  # tokenize the data
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
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
					"$SCRIPTDIR/training/trim_parallel_corpus.pl $label/$label.tok.$TARGET $label/$label.tok.$SOURCE $maxlen > $label/$label.tok.$maxlen.$TARGET 2> $label/$label.tok.$maxlen.$SOURCE",
					"$label/$label.tok.$TARGET", "$label/$label.tok.$SOURCE",
					"$label/$label.tok.$maxlen.$TARGET", "$label/$label.tok.$maxlen.$SOURCE",
		);
	$infix = ".$maxlen";
  }

  # lowercase
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
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

## returns 1 if every sentence in the corpus begins with an open paren,
## false otherwise
sub already_parsed {
  my ($corpus) = @_;

  open(CORPUS, $corpus) or die "can't read corpus file '$corpus'\n";
  while (<CORPUS>) {
	# if we see a line not beginning with an open paren, we consider
	# the file not to be parsed
	return 0 unless /^\(/;
  }
  close(CORPUS);

  return 1;
}
