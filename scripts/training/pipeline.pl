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
# - decoding with Hiero grammars and SAMT grammars

# - jump to SUBSAMPLE, ALIGN, PARSE, THRAX, MERT, and TEST points
#   (using --first-step and (optionally) --last-step)
# - built on top of CachePipe, so that intermediate results are cached
#   and only re-run if necessary
# - uses Thrax for grammar extraction

my $JOSHUA;

BEGIN {
  $JOSHUA = $ENV{JOSHUA} or not_defined("JOSHUA");
  unshift(@INC,"$ENV{JOSHUA}/scripts/training/cachepipe");
}

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use Cwd;
use POSIX qw[ceil];
use List::Util qw[max min];
use CachePipe;

my $HADOOP = undef;
my $MOSES_SCRIPTS = $ENV{SCRIPTS_ROOTDIR} or not_defined("SCRIPTS_ROOTDIR");
die not_defined("JAVA_HOME") unless exists $ENV{JAVA_HOME};

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$SOURCE,$TARGET,$LMFILE,$GRAMMAR_FILE,$GLUE_GRAMMAR_FILE,$THRAX_CONF_FILE);
my $FIRST_STEP = "FIRST";
my $LAST_STEP  = "LAST";
my $LMFILTER = "$ENV{HOME}/code/filter/filter";
my $MAXLEN = 50;
my $DO_FILTER_LM = 1;
my $DO_SUBSAMPLE = 0;
my $SCRIPTDIR = "$JOSHUA/scripts";
my $TOKENIZER = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $MOSES_TRAINER = "$MOSES_SCRIPTS/training/train-model.perl";
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

# whether to trim the grammars to each sentence
my $DO_SENT_SPECIFIC_TM = 0;

my $DO_MBR = 1;

my $ALIGNER = "giza"; # or "berkeley"

# for hadoop java subprocesses (heap amount)
# you really just have to play around to find out how much is enough 
my $HADOOP_MEM = "4g";  
my $JOSHUA_MEM = "3100m";
my $ALIGNER_MEM = "10g";
my $QSUB_ARGS  = "-l num_proc=2";
my $ALIGNER_BLOCKSIZE = 1000000;
my $NUM_JOBS = 1;
my $NUM_THREADS = 1;

my @STEPS = qw[FIRST SUBSAMPLE ALIGN PARSE THRAX MERT TEST LAST];
my %STEPS = map { $STEPS[$_] => $_ + 1 } (0..$#STEPS);

my $retval = GetOptions(
  "corpus=s" 	 	  => \@CORPORA,
  "tune=s"   	 	  => \$TUNE,
  "test=s"            => \$TEST,
  "aligner=s"         => \$ALIGNER,
  "alignment=s"  	  => \$ALIGNMENT,
  "aligner-mem=s"     => \$ALIGNER_MEM,
  "source=s"   	 	  => \$SOURCE,
  "target=s"  	 	  => \$TARGET,
  "rundir=s" 	 	  => \$RUNDIR,
  "filter-tm!"        => \$DO_SENT_SPECIFIC_TM,
  "filter-lm!"        => \$DO_FILTER_LM,
  "lmfile=s" 	 	  => \$LMFILE,
  "grammar=s"    	  => \$GRAMMAR_FILE,
  "glue-grammar=s" 	  => \$GLUE_GRAMMAR_FILE,
  "mbr!"              => \$DO_MBR,
  "type=s"       	  => \$GRAMMAR_TYPE,
  "maxlen=i" 	 	  => \$MAXLEN,
  "tokenizer=s"  	  => \$TOKENIZER,
  "joshua-config=s"   => \$MERTFILES{'joshua.config'},
  "joshua-mem=s"      => \$JOSHUA_MEM,
  "hadoop-mem=s"      => \$HADOOP_MEM,
  "decoder-command=s" => \$MERTFILES{'decoder_command'},
  "thrax-conf=s"      => \$THRAX_CONF_FILE,
  "jobs=i"            => \$NUM_JOBS,
  "threads=i"         => \$NUM_THREADS,
  "subsample!"   	  => \$DO_SUBSAMPLE,
  "qsub-args=s"  	  => \$QSUB_ARGS,
  "first-step=s" 	  => \$FIRST_STEP,
  "last-step=s"  	  => \$LAST_STEP,
  "aligner-chunk-size=s" => \$ALIGNER_BLOCKSIZE,
  "hadoop=s"          => \$HADOOP,
);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

# capitalize these to offset a common error:
$FIRST_STEP = uc($FIRST_STEP);
$LAST_STEP  = uc($LAST_STEP);

$| = 1;

my $cachepipe = new CachePipe();

$SIG{INT} = sub { 
  print "* Got C-c, quitting\n";
  $cachepipe->cleanup();
  exit 1; 
};

## Sanity Checking ###################################################

if (defined $ENV{HADOOP} and ! defined $HADOOP) {
  print "* FATAL: \$HADOOP defined (suggesting an existing hadoop\n";
  print "* FATAL: installation).  If you want to use this, pass the\n";
  print "* FATAL: directory using the --hadoop flag; if you instead want to\n";
  print "* FATAL: roll out a new cluster automatically, then unset \$HADOOP\n";
  print "* FATAL: and re-run the script.\n";
  exit;
}

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

# make sure a grammar file was given if we're skipping training
if (! defined $GRAMMAR_FILE and ($STEPS{$FIRST_STEP} >= $STEPS{MERT})) {
  print "* FATAL: need a grammar (--grammar) if you're skipping that step\n";
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

if ($ALIGNER ne "giza" and $ALIGNER ne "berkeley") {
  print "* FATAL: aligner must be one of 'giza' or 'berkeley'\n";
  exit 1;
}


## Dependent variable setting ########################################

# if parallelization is turned off, then use the sequential version of
# the decoder command
if ($NUM_JOBS == 1) {
  $MERTFILES{'decoder_command'} = "$MERTCONFDIR/decoder_command.sequential";
}

my $OOV = ($GRAMMAR_TYPE eq "samt") ? "OOV" : "X";

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

if ($TUNE) {
  $TUNE{source} = "$TUNE.$SOURCE";
  $TUNE{target} = "$TUNE.$TARGET";
}

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

if (defined $ALIGNMENT) {
  print "* FATAL: it doesn't make sense to provide an alignment and then do\n";
  print "  tokenization.  Either remove --alignment or specify a first step\n";
  print "  of Thrax (--first-step THRAX)\n";
  exit 1;
}

if (@CORPORA == 0) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

# prepare the training data
my $prefix = prepare_data("train",\@CORPORA,$MAXLEN);
$TRAIN{prefix} = "train/corpus";
foreach my $lang ($SOURCE,$TARGET) {
  system("ln -sf $prefix.$lang train/corpus.$lang");
}
$TRAIN{source} = "train/corpus.$SOURCE";
$TRAIN{target} = "train/corpus.$TARGET";

# prepare the tuning and development data
if (defined $TUNE) {
  my $prefix = prepare_data("tune",[$TUNE]);
  $TUNE{source} = "tune/$prefix.$SOURCE";
  $TUNE{target} = "tune/$prefix.$TARGET";
}

if (defined $TEST) {
  my $prefix = prepare_data("test",[$TEST]);
  $TEST{source} = "test/$prefix.$SOURCE";
  $TEST{target} = "test/$prefix.$TARGET";
}

maybe_quit("FIRST");

## SUBSAMPLE #########################################################

SUBSAMPLE:

# subsample
if ($DO_SUBSAMPLE) {
  mkdir("train/subsampled") unless -d "train/subsampled";

  $cachepipe->cmd("subsample-manifest",
				  "echo corpus > train/subsampled/manifest",
				  "train/subsampled/manifest");

  $cachepipe->cmd("subsample-testdata",
				  "cat $TUNE{source} $TEST{source} > train/subsampled/test-data",
				  $TUNE{source},
				  $TEST{source},
				  "train/subsampled/test-data");

  $cachepipe->cmd("subsample",
				  "java -Xmx4g -Dfile.encoding=utf8 -cp $JOSHUA/bin:$JOSHUA/lib/commons-cli-2.0-SNAPSHOT.jar joshua.subsample.Subsampler -e $TARGET -f $SOURCE -epath train/ -fpath train/ -output train/subsampled/subsampled.$MAXLEN -ratio 1.04 -test train/subsampled/test-data -training train/subsampled/manifest",
				  "train/subsampled/manifest",
				  "train/subsampled/test-data",
				  $TRAIN{source},
				  $TRAIN{target},
				  "train/subsampled/subsampled.$MAXLEN.$TARGET",
				  "train/subsampled/subsampled.$MAXLEN.$SOURCE");

  # rewrite the symlinks to point to the subsampled corpus
  foreach my $lang ($TARGET,$SOURCE) {
	system("ln -sf subsampled/subsampled.$MAXLEN.$lang train/corpus.$lang");
  }
}

maybe_quit("SUBSAMPLE");


## ALIGN #############################################################

ALIGN:

# This basically means that we've skipped tokenization, in which case
# we still want to move the input files into the canonical place
if ($FIRST_STEP eq "ALIGN") {
  if (defined $ALIGNMENT) {
	print "* FATAL: It doesn't make sense to provide an alignment\n";
	print "  but not to skip the tokenization and subsampling steps\n";
	exit 1;
  }

  # TODO: copy the files into the canonical place 

  # Jumping straight to alignment is probably the same thing as
  # skipping tokenization, and might also be implemented by a
  # --no-tokenization flag
}

# skip this step if an alignment was provided
if (! defined $ALIGNMENT) {

  # split up the data
  system("mkdir","-p","train/splits") unless -d "train/splits";

  $cachepipe->cmd("source-numlines",
				  "cat $TRAIN{source} | wc -l",
				  $TRAIN{source});
  my $numlines = $cachepipe->stdout();
  my $numchunks = ceil($numlines / $ALIGNER_BLOCKSIZE);

  open TARGET, $TRAIN{target} or die "can't read $TRAIN{target}";
  open SOURCE, $TRAIN{source} or die "can't read $TRAIN{source}";

  my $lastchunk = -1;
  while (my $target = <TARGET>) {
	my $source = <SOURCE>;

	# We want to prevent a very small last chunk, which we accomplish
	# by folding the last chunk into the penultimate chunk.
	my $chunk = ($numchunks <= 2)
		? 0 
		: min($numchunks - 2,
			  int( (${.} - 1) / $ALIGNER_BLOCKSIZE ));
	
	if ($chunk != $lastchunk) {
	  close CHUNK_SOURCE;
	  close CHUNK_TARGET;
	  open CHUNK_SOURCE, ">", "train/splits/corpus.$SOURCE.$chunk" or die;
	  open CHUNK_TARGET, ">", "train/splits/corpus.$TARGET.$chunk" or die;

	  $lastchunk = $chunk;
	}

	print CHUNK_SOURCE $source;
	print CHUNK_TARGET $target;
  }
  close CHUNK_SOURCE;
  close CHUNK_TARGET;

  close SOURCE;
  close TARGET;

  for (my $chunkno = 0; $chunkno <= $lastchunk; $chunkno++) {

	# create the alignment subdirectory
	my $chunkdir = "alignments/$chunkno";
	system("mkdir","-p", $chunkdir);
	  
	if ($ALIGNER eq "giza") {

	  # run the alignments commands
	  $cachepipe->cmd("giza-$chunkno",
					  "rm -f $chunkdir/corpus.0-0.*; $MOSES_TRAINER -root-dir $chunkdir -e $TARGET.$chunkno -f $SOURCE.$chunkno -corpus train/splits/corpus -first-step 1 -last-step 3 > $chunkdir/giza.log 2>&1",
					  "train/splits/corpus.$SOURCE.$chunkno",
					  "train/splits/corpus.$TARGET.$chunkno",
					  "$chunkdir/model/aligned.grow-diag-final");

	} elsif ($ALIGNER eq "berkeley") {

	  # copy and modify the config file
	  open FROM, "$JOSHUA/scripts/training/templates/alignment/word-align.conf" or die "can't read berkeley alignment template";
	  open TO, ">", "alignments/$chunkno/word-align.conf" or die "can't write to 'alignments/$chunkno/word-align.conf'";
	  while (<FROM>) {
		s/<SOURCE>/$SOURCE.$chunkno/g;
		s/<TARGET>/$TARGET.$chunkno/g;
		s/<CHUNK>/$chunkno/g;

		print TO;
	  }
	  close(TO);
	  close(FROM);

	  # run the job
	  $cachepipe->cmd("berkeley-aligner-chunk-$chunkno",
					  "java -d64 -Xmx${ALIGNER_MEM} -jar $JOSHUA/lib/berkeleyaligner.jar ++alignments/$chunkno/word-align.conf",
					  "alignments/$chunkno/word-align.conf",
					  "train/splits/corpus.$SOURCE.$chunkno",
					  "train/splits/corpus.$TARGET.$chunkno",
					  "$chunkdir/training.align");

	}
  }

  if ($ALIGNER eq "giza") {
	  # combine the alignments
	  $cachepipe->cmd("giza-aligner-combine",
					  "cat alignments/*/model/aligned.grow-diag-final > alignments/training.align",
					  "alignments/$lastchunk/model/aligned.grow-diag-final",
					  "alignments/training.align");
  } elsif ($ALIGNER eq "berkeley") {

	  # combine the alignments
	  $cachepipe->cmd("berkeley-aligner-combine",
					  "cat alignments/*/training.align > alignments/training.align",
					  "alignments/$lastchunk/training.align",
					  "alignments/training.align");
  }

  $ALIGNMENT = "alignments/training.align";
}

maybe_quit("ALIGN");


## PARSE #############################################################

PARSE:

mkdir("train") unless -d "train";

if ($GRAMMAR_TYPE eq "samt") {

  $cachepipe->cmd("build-vocab",
				  "cat $TRAIN{target} | $SCRIPTDIR/training/build-vocab.pl > train/vocab.$TARGET",
				  $TRAIN{target},
				  "train/vocab.$TARGET");

  $cachepipe->cmd("parse",
				  "cat $TRAIN{target} | java -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr | sed 's/^\(/\(TOP/' | tee train/corpus.$TARGET.parsed.mc | perl -pi -e 's/(\\S+)\\)/lc(\$1).\")\"/ge' | tee train/corpus.$TARGET.parsed | perl $SCRIPTDIR/training/add-OOVs.pl train/vocab.$TARGET > train/corpus.parsed.$TARGET",
				  "$TRAIN{target}",
				  "train/corpus.parsed.$TARGET");

  $TRAIN{parsed} = "train/corpus.parsed.$TARGET";
}


## THRAX #############################################################

THRAX:

if ($GRAMMAR_TYPE eq "samt") {

  # if we jumped right here, $TRAIN{target} should be parsed
  if (exists $TRAIN{parsed}) {
	# parsing step happened in-script, all is well

  } elsif (already_parsed($TRAIN{target})) {
	# skipped straight to this step, passing a parsed corpus

	mkdir("train") unless -d "train";

	$TRAIN{parsed} = "train/corpus.parsed.$TARGET";
	
	$cachepipe->cmd("cp-train-$TARGET",
					"cp $TRAIN{target} $TRAIN{parsed}",
					$TRAIN{target}, 
					$TRAIN{parsed});

	$TRAIN{target} = "train/corpus.$TARGET";

	# now extract the leaves of the parsed corpus
	$cachepipe->cmd("extract-leaves",
                 "cat $TRAIN{parsed} | perl -pe 's/\\(.*?(\\S\+)\\)\+?/\$1/g' | perl -pe 's/\\)//g' > $TRAIN{target}",
				 $TRAIN{parsed},
				 $TRAIN{target});

	if ($TRAIN{source} ne "train/corpus.$SOURCE") {
	  $cachepipe->cmd("cp-train-$SOURCE",
					  "cp $TRAIN{source} train/corpus.$SOURCE",
					  $TRAIN{source}, "train/corpus.$SOURCE");
	  $TRAIN{source} = "train/corpus.$SOURCE";
	}

  } else {

	print "* FATAL: You requested to build an SAMT grammar, but provided an\n";
	print "  unparsed corpus.  Please re-run the pipeline and begin no later\n";
	print "  than the PARSE step (--first-step PARSE)\n";
	exit 1;
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
  my $target_file = ($GRAMMAR_TYPE eq "hiero") 
	  ? $TRAIN{target} : $TRAIN{parsed};
  $cachepipe->cmd("thrax-input-file",
				  "paste $TRAIN{source} $target_file $ALIGNMENT | perl -pe 's/\\t/ ||| /g' | grep -v '(())' > train/thrax-input-file",
					$TRAIN{source}, $target_file, $ALIGNMENT,
					"train/thrax-input-file");


  # rollout the hadoop cluster if needed
  start_hadoop_cluster() unless defined $HADOOP;

  # put the hadoop files in place
  my $THRAXDIR = "pipeline-$SOURCE-$TARGET-$GRAMMAR_TYPE-$RUNDIR";
  $THRAXDIR =~ s#/#_#g;

  $cachepipe->cmd("thrax-prep",
				  "$HADOOP/bin/hadoop fs -rmr $THRAXDIR; $HADOOP/bin/hadoop fs -mkdir $THRAXDIR; $HADOOP/bin/hadoop fs -put train/thrax-input-file $THRAXDIR/input-file",
				  "train/thrax-input-file", 
				  "grammar.gz");

  # copy the thrax config file
  system("grep -v input-file $THRAX_CONF_FILE > thrax-$GRAMMAR_TYPE.conf");
  system("echo input-file $THRAXDIR/input-file >> thrax-$GRAMMAR_TYPE.conf");

  $cachepipe->cmd("thrax-run",
				  "$HADOOP/bin/hadoop jar $JOSHUA/lib/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' thrax-$GRAMMAR_TYPE.conf $THRAXDIR > thrax.log 2>&1; rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $THRAXDIR/final/ grammar; gzip -9f grammar",
				  "train/thrax-input-file",
				  "thrax-$GRAMMAR_TYPE.conf",
				  "grammar.gz");

  stop_hadoop_cluster() if $HADOOP eq "hadoop";

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
	print STDERR "* FATAL: can't find lmfile '$LMFILE'\n";
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
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | java -Dfile.encoding=utf8 -cp $JOSHUA/lib/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $TUNE{source} | $SCRIPTDIR/training/remove-unary-abstract.pl | gzip -9 > tune/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TUNE{source},
				"tune/grammar.filtered.gz");

# copy the thrax config file if it's not already there
if (! defined $GLUE_GRAMMAR_FILE) {
  system("grep -v input-file $THRAX_CONF_FILE > thrax-$GRAMMAR_TYPE.conf")
	  unless -e "thrax-$GRAMMAR_TYPE.conf";
  $cachepipe->cmd("glue-tune",
				  "$SCRIPTDIR/training/scat tune/grammar.filtered.gz | java -cp $JOSHUA/lib/thrax.jar:$JOSHUA/lib/hadoop-core-0.20.203.0.jar:$JOSHUA/lib/commons-logging-1.1.1.jar edu.jhu.thrax.util.CreateGlueGrammar thrax-$GRAMMAR_TYPE.conf > tune/grammar.glue",
				  "tune/grammar.filtered.gz",
				  "tune/grammar.glue");
  $GLUE_GRAMMAR_FILE = "tune/grammar.glue";
} else {
  $cachepipe->cmd("glue-tune-copy",
				  "cp $GLUE_GRAMMAR_FILE tune/grammar.glue",
				  $GLUE_GRAMMAR_FILE,
				  "tune/grammar.glue");
}
	 

# figure out how many references there are
my $numrefs = get_numrefs($TUNE{target});

mkdir("mert") unless -d "mert";
foreach my $key (keys %MERTFILES) {
  my $file = $MERTFILES{$key};
  open FROM, $file or die "can't find file '$file'";
  open TO, ">mert/$key" or die "can't write to file 'mert/$key'";
  while (<FROM>) {
	s/<INPUT>/$TUNE{source}/g;
	s/<SOURCE>/$SOURCE/g;
	s/<RUNDIR>/$RUNDIR/g;
	s/<TARGET>/$TARGET/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR_TYPE/g;
	s/<OOV>/$OOV/g;
	s/<NUMJOBS>/$NUM_JOBS/g;
	s/<NUMTHREADS>/$NUM_THREADS/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/mert\/tune.output.nbest/g;
	s/<REF>/$TUNE{target}/g;
	s/<JOSHUA>/$JOSHUA/g;
	s/<NUMREFS>/$numrefs/g;
	s/<CONFIG>/mert\/joshua.config/g;
	s/<LOG>/mert\/joshua.log/g;
	s/use_sent_specific_tm=.*/use_sent_specific_tm=$DO_SENT_SPECIFIC_TM/;

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

# If we're not quitting at this step, then copy the final Joshua
# config file to the test directory.
if ($LAST_STEP ne "MERT") {
  mkdir("test") unless -d "test";

  # for testing, mark OOVs, don't keep sentence-specific grammars
  $cachepipe->cmd("test-joshua-config-from-mert",
				  "cat mert/joshua.config.ZMERT.final | perl -pe 's#tune/#test/#; s/mark_oovs=false/mark_oovs=true/; s/use_sent_specific_tm=.*/use_sent_specific_tm=$DO_SENT_SPECIFIC_TM/; s/keep_sent_specific_tm=true/keep_sent_specific_tm=false/' > test/joshua.config",
				  "mert/joshua.config.ZMERT.final",
				  "test/joshua.config");
}

## Decode the test set
TEST:

mkdir("test") unless -d "test";

# If we jumped directly to this step, then the caller is required to
# have specified a Joshua config file (fully instantiated, not a
# template), which we'll copy in place
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
				"$SCRIPTDIR/training/scat $GRAMMAR_FILE | java -Dfile.encoding=utf8 -cp $JOSHUA/lib/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $TEST{source} | $SCRIPTDIR/training/remove-unary-abstract.pl | gzip -9 > test/grammar.filtered.gz",
				$GRAMMAR_FILE,
				$TEST{source},
				"test/grammar.filtered.gz");

# copy the thrax config file if it's not already there
if (! defined $GLUE_GRAMMAR_FILE) {
  system("grep -v input-file $THRAX_CONF_FILE > thrax-$GRAMMAR_TYPE.conf")
	  unless -e "thrax-$GRAMMAR_TYPE.conf";

  $cachepipe->cmd("glue-test",
				  "$SCRIPTDIR/training/scat test/grammar.filtered.gz | java -cp $JOSHUA/lib/thrax.jar:$JOSHUA/hadoop-core-20.203.0.jar:$JOSHUA/lib/commons-logging-1.1.1.jar edu.jhu.thrax.util.CreateGlueGrammar thrax-$GRAMMAR_TYPE.conf > test/grammar.glue",
				  "test/grammar.filtered.gz",
				  "test/grammar.glue");
  $GLUE_GRAMMAR_FILE = "test/grammar.glue";
} else {
  $cachepipe->cmd("glue-test-copy",
				  "cp $GLUE_GRAMMAR_FILE test/grammar.glue",
				  $GLUE_GRAMMAR_FILE,
				  "test/grammar.glue");
}

# decode test set
foreach my $key (qw(decoder_command)) {
  my $file = $MERTFILES{$key};
  open FROM, $file or die "can't find file '$file'";
  open TO, ">test/$key" or die "can't write to 'test/$key'";
  while (<FROM>) {
	s/<INPUT>/$TEST{source}/g;
	s/<NUMJOBS>/$NUM_JOBS/g;
	s/<NUMTHREADS>/$NUM_THREADS/g;
	s/<QSUB_ARGS>/$QSUB_ARGS/g;
	s/<OUTPUT>/test\/test.output.nbest/g;
	s/<JOSHUA>/$JOSHUA/g;
	s/<NUMREFS>/$numrefs/g;
	s/<SOURCE>/$SOURCE/g;
	s/<TARGET>/$TARGET/g;
	s/<RUNDIR>/$TARGET/g;
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

$cachepipe->cmd("remove-oov",
				"cat test/test.output.nbest | perl -pe 's/_OOV//g' > test/test.output.nbest.noOOV",
				"test/test.output.nbest",
				"test/test.output.nbest.noOOV");

if ($DO_MBR) {
  my $numlines = `cat $TEST{source} | wc -l`;
  $numlines--;

  $cachepipe->cmd("test-onebest-parmbr", 
				  "cat test/test.output.nbest.noOOV | java -Xmx1700m -cp $JOSHUA/bin joshua.decoder.NbestMinRiskReranker false 1 > test/test.output.1best",
				  "test/test.output.nbest.noOOV", 
				  "test/test.output.1best");
} else {
  $cachepipe->cmd("test-extract-onebest",
				  "java -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.util.ExtractTopCand test/test.output.nbest test/test.output.1best",
				  "test/test.output.nbest.noOOV", 
				  "test/test.output.1best");
}

$numrefs = get_numrefs($TEST{target});
$cachepipe->cmd("test-bleu",
				"java -cp $JOSHUA/bin -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand test/test.output.1best -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > test/test.output.1best.bleu",
				"test/test.output.1best", "test/test.output.1best.bleu");

system("cat test/test.output.1best.bleu");
				
######################################################################
## SUBROUTINES #######################################################
######################################################################
LAST:
1;

# Does tokenization and normalization of training, tuning, and test data.
# $label: one of train, tune, or test
# $corpora: arrayref of files (multiple allowed for training data)
# $maxlen: maximum length (only applicable to training)
sub prepare_data {
  my ($label,$corpora,$maxlen) = @_;

  mkdir $label unless -d $label;

  # copy the data from its original location to our location
  foreach my $ext ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
	# append each extension to the corpora prefixes
	my @files = map { "$_.$ext" } @$corpora;
	# a list of all the files (in case of multiple corpora prefixes)
	my $files = join(" ",@files);
	if (-e $files[0]) {
	  $cachepipe->cmd("$label-copy-$ext",
					  "cat $files | gzip -9 > $label/$label.$ext.gz",
					  @files, "$label/$label.$ext.gz");
	}
  }

  my $prefix = "$label";

  # tokenize the data
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
	if (-e "$label/$prefix.$lang.gz") {
	  $cachepipe->cmd("$label-tokenize-$lang",
					  "$SCRIPTDIR/training/scat $label/$prefix.$lang.gz | $TOKENIZER -l $lang 2> /dev/null | gzip -9 > $label/$prefix.tok.$lang.gz",
					  "$label/$prefix.$lang.gz", "$label/$prefix.tok.$lang.gz"
		  );
	  # extend the prefix
	}
  }
  $prefix .= ".tok";

  if ($label eq "train") {
	if ($maxlen) {
	  # trim training data
	  $cachepipe->cmd("train-trim",
					  "paste <(gzip -cd $label/$prefix.$TARGET.gz) <(gzip -cd $label/$prefix.$SOURCE.gz) | $SCRIPTDIR/training/trim_parallel_corpus.pl $maxlen | $SCRIPTDIR/training/split2files.pl $label/$prefix.$maxlen.$TARGET.gz $label/$prefix.$maxlen.$SOURCE.gz",
					  "$label/$prefix.$TARGET.gz", 
					  "$label/$prefix.$SOURCE.gz",
					  "$label/$prefix.$maxlen.$TARGET.gz", 
					  "$label/$prefix.$maxlen.$SOURCE.gz",
		  );
	}
	$prefix .= ".$maxlen";
  }

  # lowercase
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
	if (-e "$label/$prefix.$lang.gz") {
	  $cachepipe->cmd("$label-lowercase-$lang",
					  "gzip -cd $label/$prefix.$lang.gz | $SCRIPTDIR/lowercase.perl > $label/$prefix.lc.$lang",
					  "$label/$prefix.$lang.gz",
					  "$label/$prefix.lc.$lang");
	}
  }
  $prefix .= ".lc";

  return $prefix;
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

sub not_defined {
  my ($var) = @_;

  print "* FATAL: environment variable \$$var is not defined.\n";
  exit;
}

# Takes a prefix.  If that prefix exists, then all the references are
# assumed to be in that file.  Otherwise, we successively append an
# index, looking for parallel references.
sub get_numrefs {
  my ($prefix) = @_;

  my $numrefs = 1;
  if (! -e $prefix) {
	my $index = 0;
	while (-e "$prefix.$index") {
	  $index++;
	}
	$numrefs = $index;
  }

  return $numrefs;
}

sub start_hadoop_cluster {
  rollout_hadoop_cluster();

  # start the cluster
  system("./hadoop/bin/start-all.sh");
  sleep(120);
}

sub rollout_hadoop_cluster {
  # if it's not already unpacked, unpack it
  if (! -d "hadoop") {
	system("tar xzf $JOSHUA/lib/hadoop-0.20.203.0rc1.tar.gz");
	system("ln -sf hadoop-0.20.203.0 hadoop");

	chomp(my $hostname = `hostname -f`);

	# copy configuration files
	foreach my $file (qw/core-site.xml mapred-site.xml hdfs-site.xml/) {
	  open READ, "$JOSHUA/scripts/training/templates/hadoop/$file" or die $file;
	  open WRITE, ">", "hadoop/conf/$file" or die "write $file";
	  while (<READ>) {
		s/<HADOOP-TMP-DIR>/$RUNDIR\/hadoop\/tmp/g;
		s/<HOST>/$hostname/g;
		s/<PORT1>/9000/g;
		s/<PORT2>/9001/g;
		s/<MAX-MAP-TASKS>/2/g;
		s/<MAX-REDUCE-TASKS>/2/g;

		print WRITE;
	  }
	  close WRITE;
	  close READ;
	}

	system("echo $hostname > hadoop/conf/masters");
	system("echo $hostname > hadoop/conf/slaves");
  system("echo export JAVA_HOME=$ENV{JAVA_HOME} >> hadoop/conf/hadoop-env.sh");

  } else {

	# if it exists, shut things down, just in case
	system("./hadoop/bin/stop-all.sh");

  }
  
  # make sure hadoop isn't running already
  my $running = `ps ax | grep hadoop | grep -v grep`;
  if ($running) {
	print "* WARNING: it looks like some Hadoop processes are already running\n";
	$running =~ s/^/\t/gm;
	print $running;
  }

  # format the name node
  system("./hadoop/bin/hadoop namenode -format");
  sleep(120);

  $ENV{HADOOP} = $HADOOP = "hadoop";
}

sub stop_hadoop_cluster {
  system("hadoop/bin/stop-all.sh");
}

sub teardown_hadoop_cluster {
  stop_hadoop_cluster();
  system("rm -rf hadoop-0.20.203.0 hadoop");
}
