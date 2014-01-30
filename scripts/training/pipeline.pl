#!/usr/bin/perl

# This script implements the Joshua pipeline.  It can run a complete
# pipeline --- from raw training corpora to bleu scores on a test set
# --- and it allows jumping into arbitrary points of the pipeline. 

my $JOSHUA;

BEGIN {
  if (! exists $ENV{JOSHUA} || $ENV{JOSHUA} eq "" ||
      ! exists $ENV{JAVA_HOME} || $ENV{JAVA_HOME} eq "") {
                print "Several environment variables must be set before running the pipeline.  Please set:\n";
                print "* \$JOSHUA to the root of the Joshua source code.\n"
                                if (! exists $ENV{JOSHUA} || $ENV{JOSHUA} eq "");
                print "* \$JAVA_HOME to the directory of your local java installation. \n"
                                if (! exists $ENV{JAVA_HOME} || $ENV{JAVA_HOME} eq "");
                exit;
  }
  $JOSHUA = $ENV{JOSHUA};
  unshift(@INC,"$JOSHUA/scripts/training/cachepipe");
  unshift(@INC,"$JOSHUA/lib");
}

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use Cwd qw[abs_path getcwd];
use POSIX qw[ceil];
use List::Util qw[max min sum];
use File::Temp qw[:mktemp];
use CachePipe;
# use Thread::Pool;

# Hadoop uses a stupid hacker trick to change directories, but (per Lane Schwartz) if CDPATH
# contains ".", it triggers the printing of the directory, which kills the stupid hacker trick.
# Thus we undefine CDPATH to ensure this doesn't happen.
delete $ENV{CDPATH};

my $HADOOP = $ENV{HADOOP};
my $MOSES = $ENV{MOSES};
delete $ENV{GREP_OPTIONS};

my $THRAX = "$JOSHUA/thrax";

die not_defined("JAVA_HOME") unless exists $ENV{JAVA_HOME};

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$SOURCE,$TARGET,@LMFILES,$GRAMMAR_FILE,$GLUE_GRAMMAR_FILE,$TUNE_GRAMMAR_FILE,$TEST_GRAMMAR_FILE,$THRAX_CONF_FILE);
my $FIRST_STEP = "FIRST";
my $LAST_STEP  = "LAST";
my $LMFILTER = "$ENV{HOME}/code/filter/filter";

# The maximum length of training sentences (--maxlen). The threshold is applied to both sides.
my $MAXLEN = 50;

# The maximum span rules in the main grammar can be applied to
my $MAXSPAN = 20;

# The maximum length of tuning and testing sentences (--maxlen-tune and --maxlen-test).
my $MAXLEN_TUNE = 0;
my $MAXLEN_TEST = 0;

my $DO_FILTER_TM = 1;
my $DO_SUBSAMPLE = 0;
my $DO_PACK_GRAMMARS = 1;
my $SCRIPTDIR = "$JOSHUA/scripts";
my $TOKENIZER_SOURCE = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $TOKENIZER_TARGET = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $NORMALIZER = "$SCRIPTDIR/training/normalize-punctuation.pl";
my $GIZA_TRAINER = "$SCRIPTDIR/training/run-giza.pl";
my $TUNECONFDIR = "$SCRIPTDIR/training/templates/tune";
my $SRILM = ($ENV{SRILM}||"")."/bin/i686-m64/ngram-count";
my $COPY_CONFIG = "$SCRIPTDIR/copy-config.pl";
my $STARTDIR;
my $RUNDIR = $STARTDIR = getcwd();
my $GRAMMAR_TYPE = "hiero";  # or "phrasal" or "samt" or "ghkm"

# Which GHKM extractor to use ("galley" or "moses")
my $GHKM_EXTRACTOR = "moses";
my $EXTRACT_OPTIONS = "";

my $WITTEN_BELL = 0;

my $JOSHUA_ARGS = "";

# Run description.
my $README = undef;

# gzip-aware cat
my $CAT = "$SCRIPTDIR/training/scat";

# where processed data files are stored
my $DATA_DIR = "data";

# this file should exist in the Joshua mert templates file; it contains
# the Joshua command invoked by MERT
my $JOSHUA_CONFIG_ORIG   = "$TUNECONFDIR/joshua.config";
my %TUNEFILES = (
  'decoder_command' => "$TUNECONFDIR/decoder_command.qsub",
  'joshua.config'   => $JOSHUA_CONFIG_ORIG,
  'mert.config'     => "$TUNECONFDIR/mert.config",
  'pro.config'      => "$TUNECONFDIR/pro.config",
  'params.txt'      => "$TUNECONFDIR/params.txt",
);

# Whether to do MBR decoding on the n-best list (for test data).
my $DO_MBR = 0;

# Which aligner to use. The options are "giza" or "berkeley".
my $ALIGNER = "giza"; # "berkeley" or "giza" or "jacana"

# Filter rules to the following maximum scope (Hopkins & Langmead, 2011).
my $SCOPE = 3;

# What kind of filtering to use ("fast" or "exact").
my $FILTERING = "fast";

# This is the amount of memory made available to Joshua.  You'll need
# a lot more than this for SAMT decoding (though really it depends
# mostly on your grammar size)
my $JOSHUA_MEM = "3100m";

# the amount of memory available for hadoop processes (passed to
# Hadoop via -Dmapred.child.java.opts
my $HADOOP_MEM = "2g";

# The location of a custom core-site.xml file, if desired (optional).
my $HADOOP_CONF = undef;

# memory available to the parser
my $PARSER_MEM = "2g";

# memory available for building the language model
my $BUILDLM_MEM = "2G";

# Memory available for packing the grammar.
my $PACKER_MEM = "8g";

# When qsub is called for decoding, these arguments should be passed to it.
my $QSUB_ARGS  = "";

# When qsub is called for aligning, these arguments should be passed to it.
my $QSUB_ALIGN_ARGS  = "-l h_rt=168:00:00,h_vmem=15g,mem_free=10g,num_proc=1";

# Amount of memory for the Berkeley aligner.
my $ALIGNER_MEM = "10g";

# Align corpus files a million lines at a time.
my $ALIGNER_BLOCKSIZE = 1000000;

# The number of machines to decode on.  If you set this higher than 1,
# you need to have qsub configured for your environment.
my $NUM_JOBS = 1;

# The number of threads to use at different pieces in the pipeline
# (giza, decoding)
my $NUM_THREADS = 1;

# which LM to use (kenlm or berkeleylm)
my $LM_TYPE = "kenlm";

# n-gram order
my $LM_ORDER = 5;

# Whether to build and include an LM from the target-side of the
# corpus when manually-specified LM files are passed with --lmfile.
my $DO_BUILD_LM_FROM_CORPUS = 1;

# whether to tokenize and lowercase training, tuning, and test data
my $DO_PREPARE_CORPORA = 1;

# how many optimizer runs to perform
my $OPTIMIZER_RUNS = 1;

# what to use to create language models ("berkeleylm" or "srilm")
my $LM_GEN = "kenlm";

my @STEPS = qw[FIRST SUBSAMPLE ALIGN PARSE THRAX GRAMMAR TUNE MERT PRO TEST LAST];
my %STEPS = map { $STEPS[$_] => $_ + 1 } (0..$#STEPS);

my $NAME = undef;

# Methods to use for merging alignments (see Koehn et al., 2003).
# Options are union, {intersect, grow, srctotgt, tgttosrc}-{diag,final,final-and,diag-final,diag-final-and}
my $GIZA_MERGE = "grow-diag-final";

# Whether to merge all the --lmfile LMs into a single LM using weights based on the development corpus
my $MERGE_LMS = 0;

# Which tuner to use by default
my $TUNER = "mert";  # or "pro" or "mira"

# The number of iterations of the mira to run
my $MIRA_ITERATIONS = 15;

# location of already-parsed corpus
my $PARSED_CORPUS = undef;

# Allows the user to set a temp dir for various tasks
my $TMPDIR = "/tmp";

# Enable forest rescoring
my $RESCORE_FOREST = 0;
my $LM_STATE_MINIMIZATION = "true";

my $NBEST = 300;

my $retval = GetOptions(
  "readme=s"    => \$README,
  "corpus=s"        => \@CORPORA,
  "parsed-corpus=s"   => \$PARSED_CORPUS,
  "tune=s"          => \$TUNE,
  "test=s"            => \$TEST,
  "prepare!"          => \$DO_PREPARE_CORPORA,
  "name=s"            => \$NAME,
  "aligner=s"         => \$ALIGNER,
  "alignment=s"      => \$ALIGNMENT,
  "aligner-mem=s"     => \$ALIGNER_MEM,
  "giza-merge=s"      => \$GIZA_MERGE,
  "source=s"          => \$SOURCE,
  "target=s"         => \$TARGET,
  "rundir=s"        => \$RUNDIR,
  "filter-tm!"        => \$DO_FILTER_TM,
  "scope=i"           => \$SCOPE,
  "filtering=s"       => \$FILTERING,
  "lm=s"              => \$LM_TYPE,
  "lmfile=s"        => \@LMFILES,
  "merge-lms!"        => \$MERGE_LMS,
  "lm-gen=s"          => \$LM_GEN,
  "lm-order=i"        => \$LM_ORDER,
  "corpus-lm!"        => \$DO_BUILD_LM_FROM_CORPUS,
  "witten-bell!"     => \$WITTEN_BELL,
  "tune-grammar=s"    => \$TUNE_GRAMMAR_FILE,
  "test-grammar=s"    => \$TEST_GRAMMAR_FILE,
  "grammar=s"        => \$GRAMMAR_FILE,
  "glue-grammar=s"     => \$GLUE_GRAMMAR_FILE,
  "maxspan=i"         => \$MAXSPAN,
  "mbr!"              => \$DO_MBR,
  "type=s"           => \$GRAMMAR_TYPE,
  "ghkm-extractor=s"  => \$GHKM_EXTRACTOR,
  "extract-options=s" => \$EXTRACT_OPTIONS,
  "maxlen=i"        => \$MAXLEN,
  "maxlen-tune=i"        => \$MAXLEN_TUNE,
  "maxlen-test=i"        => \$MAXLEN_TEST,
  "tokenizer-source=s"      => \$TOKENIZER_SOURCE,
  "tokenizer-target=s"      => \$TOKENIZER_TARGET,
  "joshua-config=s"   => \$TUNEFILES{'joshua.config'},
  "joshua-args=s"      => \$JOSHUA_ARGS,
  "joshua-mem=s"      => \$JOSHUA_MEM,
  "hadoop-mem=s"      => \$HADOOP_MEM,
  "parser-mem=s"      => \$PARSER_MEM,
  "buildlm-mem=s"     => \$BUILDLM_MEM,
  "packer-mem=s"      => \$PACKER_MEM,
  "pack!"             => \$DO_PACK_GRAMMARS,
  "decoder-command=s" => \$TUNEFILES{'decoder_command'},
  "tuner=s"           => \$TUNER,
  "mira-iterations=i" => \$MIRA_ITERATIONS,
  "thrax=s"           => \$THRAX,
  "thrax-conf=s"      => \$THRAX_CONF_FILE,
  "jobs=i"            => \$NUM_JOBS,
  "threads=i"         => \$NUM_THREADS,
  "subsample!"       => \$DO_SUBSAMPLE,
  "qsub-args=s"      => \$QSUB_ARGS,
  "qsub-align-args=s"      => \$QSUB_ALIGN_ARGS,
  "first-step=s"     => \$FIRST_STEP,
  "last-step=s"      => \$LAST_STEP,
  "aligner-chunk-size=s" => \$ALIGNER_BLOCKSIZE,
  "hadoop=s"          => \$HADOOP,
  "hadoop-conf=s"          => \$HADOOP_CONF,
  "optimizer-runs=i"  => \$OPTIMIZER_RUNS,
  "tmp=s"             => \$TMPDIR,
  "rescore-forest!"  => \$RESCORE_FOREST,
  "nbest=i"           => \$NBEST,
);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

# Forest rescoring doesn't work with LM state minimization
if ($RESCORE_FOREST) {
  $LM_STATE_MINIMIZATION = "false";
}

$RUNDIR = get_absolute_path($RUNDIR);

$TUNER = lc $TUNER;

my $DOING_LATTICES = 0;

# Prepend a space to the arguments list if it's non-empty and doesn't already have the space.
if ($JOSHUA_ARGS ne "" and $JOSHUA_ARGS !~ /^\s/) {
  $JOSHUA_ARGS = " $JOSHUA_ARGS";
}

$TUNEFILES{'joshua.config'} = get_absolute_path($TUNEFILES{'joshua.config'});
$TUNEFILES{'decoder_command'} = get_absolute_path($TUNEFILES{'decoder_command'});

my %DATA_DIRS = (
  train => get_absolute_path("$RUNDIR/$DATA_DIR/train"),
  tune  => get_absolute_path("$RUNDIR/$DATA_DIR/tune"),
  test  => get_absolute_path("$RUNDIR/$DATA_DIR/test"),
);

if (defined $NAME) {
  map { $DATA_DIRS{$_} .= "/$NAME" } (keys %DATA_DIRS);
}

# capitalize these to offset a common error:
$FIRST_STEP = uc($FIRST_STEP);
$LAST_STEP  = uc($LAST_STEP);

$| = 1;

my $cachepipe = new CachePipe();

# This tells cachepipe not to include the command signature when determining to run a command.  Note
# that this is not backwards compatible!
$cachepipe->omit_cmd();

$SIG{INT} = sub { 
  print "* Got C-c, quitting\n";
  $cachepipe->cleanup();
  exit 1; 
};

# if no LMs were specified, we need to build one from the target side of the corpus
if (scalar @LMFILES == 0) {
  $DO_BUILD_LM_FROM_CORPUS = 1;
}

## Sanity Checking ###################################################

# If a language model was specified and no corpus was given to build another one from the target
# side of the training data (which could happen, for example, when starting at the tuning step with
# an existing LM), turn off building an LM from the corpus.  The user could have done this
# explicitly with --no-corpus-lm, but might have forgotten to, and we con't want to pester them with
# an error about easily-inferrable intentions.
if (scalar @LMFILES && ! scalar(@CORPORA)) {
  $DO_BUILD_LM_FROM_CORPUS = 0;
}


# if merging LMs, make sure there are at least 2 LMs to merge.
# first, pin $DO_BUILD_LM_FROM_CORPUS to 0 or 1 so that the subsequent check works.
if ($MERGE_LMS) {
  if ($DO_BUILD_LM_FROM_CORPUS != 0) {
    $DO_BUILD_LM_FROM_CORPUS = 1
  }

  if (@LMFILES + $DO_BUILD_LM_FROM_CORPUS < 2) {
    print "* FATAL: I need 2 or more language models to merge (including the corpus target-side LM).";
    exit 2;
  }
}

# absolutize LM file paths
map {
  $LMFILES[$_] = get_absolute_path($LMFILES[$_]);
} 0..$#LMFILES;

# make sure the LMs exist
foreach my $lmfile (@LMFILES) {
  if (! -e $lmfile) {
    print "* FATAL: couldn't find language model file '$lmfile'\n";
    exit 1;
  }
}

# case-normalize this
$GRAMMAR_TYPE = lc $GRAMMAR_TYPE;

# make sure source and target were specified
if (! defined $SOURCE or $SOURCE eq "") {
  print "* FATAL: I need a source language extension (--source)\n";
  exit 1;
}
if (! defined $TARGET or $TARGET eq "") {
  print "* FATAL: I need a target language extension (--target)\n";
  exit 1;
}

# make sure a corpus was provided if we're doing any step before tuning
if (@CORPORA == 0 and $STEPS{$FIRST_STEP} < $STEPS{TUNE}) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

# make sure a tuning corpus was provided if we're doing tuning
if (! defined $TUNE and ($STEPS{$FIRST_STEP} <= $STEPS{TUNE}
                         and $STEPS{$LAST_STEP} >= $STEPS{TUNE})) { 
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
if (! defined $GRAMMAR_FILE) {
  if ($STEPS{$FIRST_STEP} >= $STEPS{TEST}) {
    if (! defined $TEST_GRAMMAR_FILE) {
      print "* FATAL: need a grammar (--grammar or --test-grammar) if you're skipping to testing\n";
			exit 1;
		}
  } elsif ($STEPS{$FIRST_STEP} >= $STEPS{TUNE}) {
		if (! defined $TUNE_GRAMMAR_FILE) {
			print "* FATAL: need a grammar (--grammar or --tune-grammar) if you're skipping grammar learning\n";
			exit 1;
		}
  }
}

# make sure SRILM is defined if we're building a language model
if ($LM_GEN eq "srilm" && (scalar @LMFILES == 0) && $STEPS{$FIRST_STEP} <= $STEPS{TUNE} && $STEPS{$LAST_STEP} >= $STEPS{TUNE}) {
  not_defined("SRILM") unless exists $ENV{SRILM} and -d $ENV{SRILM};
}

# check for file presence
if (defined $GRAMMAR_FILE and ! -e $GRAMMAR_FILE) {
  print "* FATAL: couldn't find grammar file '$GRAMMAR_FILE'\n";
  exit 1;
}
if (defined $TUNE_GRAMMAR_FILE and ! -e $TUNE_GRAMMAR_FILE) {
  print "* FATAL: couldn't find tuning grammar file '$TUNE_GRAMMAR_FILE'\n";
  exit 1;
}
if (defined $TEST_GRAMMAR_FILE and ! -e $TEST_GRAMMAR_FILE) {
  print "* FATAL: couldn't find test grammar file '$TEST_GRAMMAR_FILE'\n";
  exit 1;
}
if (defined $ALIGNMENT and ! -e $ALIGNMENT) {
  print "* FATAL: couldn't find alignment file '$ALIGNMENT'\n";
  exit 1;
}

# If $CORPUS was a relative path, prepend the starting directory (under the assumption it was
# relative to there).  This makes sure that everything will still work if we change the run
# directory.
map {
  $CORPORA[$_] = get_absolute_path("$CORPORA[$_]");
} (0..$#CORPORA);

# Do the same for tuning and test data, and other files
$TUNE = get_absolute_path($TUNE);
$TEST = get_absolute_path($TEST);

$GRAMMAR_FILE = get_absolute_path($GRAMMAR_FILE);
$GLUE_GRAMMAR_FILE = get_absolute_path($GLUE_GRAMMAR_FILE);
$TUNE_GRAMMAR_FILE = get_absolute_path($TUNE_GRAMMAR_FILE);
$TEST_GRAMMAR_FILE = get_absolute_path($TEST_GRAMMAR_FILE);
$THRAX_CONF_FILE = get_absolute_path($THRAX_CONF_FILE);
$ALIGNMENT = get_absolute_path($ALIGNMENT);
$HADOOP_CONF = get_absolute_path($HADOOP_CONF);

foreach my $corpus (@CORPORA) {
  foreach my $ext ($TARGET,$SOURCE) {
    if (! -e "$corpus.$ext") {
      print "* FATAL: can't find '$corpus.$ext'";
      exit 1;
    } 
  }
}

if ($ALIGNER ne "giza" and $ALIGNER ne "berkeley" and $ALIGNER ne "jacana") {
  print "* FATAL: aligner must be one of 'giza', 'berkeley' or 'jacana' (only French-English)\n";
  exit 1;
}

if ($LM_TYPE ne "kenlm" and $LM_TYPE ne "berkeleylm") {
  print "* FATAL: lm type (--lm) must be one of 'kenlm' or 'berkeleylm'\n";
  exit 1;
}

if ($LM_GEN ne "berkeleylm" and $LM_GEN ne "srilm" and $LM_GEN ne "kenlm") {
  print "* FATAL: lm generating code (--lm-gen) must be one of 'kenlm' (default), 'berkeleylm', or 'srilm'\n";
  exit 1;
}

if ($TUNER eq "mira") {
  if (! defined $MOSES) {
    print "* FATAL: using MIRA for tuning requires setting the MOSES environment variable\n";
    exit 1;
  }
}

if ($TUNER ne "mert" and $TUNER ne "mira" and $TUNER ne "pro") {
  print "* FATAL: --tuner must be one of 'mert', 'pro', or 'mira'.\n";
  exit 1;
}

$FILTERING = lc $FILTERING;
if ($FILTERING eq "fast") {
  $FILTERING = "-f"
} elsif ($FILTERING eq "exact") {
  $FILTERING = "-e";
} elsif ($FILTERING eq "loose") {
  $FILTERING = "-l";
} else {
  print "* FATAL: --filtering must be one of 'fast' (default) or 'exact' or 'loose'\n";
  exit 1;
}

if (defined $HADOOP_CONF && ! -e $HADOOP_CONF) {
  print STDERR "* FATAL: Couldn't find \$HADOOP_CONF file '$HADOOP_CONF'\n";
  exit 1;
}

## END SANITY CHECKS

####################################################################################################
## Dependent variable setting ######################################################################
####################################################################################################

# if parallelization is turned off, then use the sequential version of
# the decoder command
if ($NUM_JOBS == 1) {
  $TUNEFILES{'decoder_command'} = "$TUNECONFDIR/decoder_command.sequential";
}

my $OOV = ($GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "phrasal") ? "X" : "OOV";

# The phrasal system should use the ITG grammar, allowing for limited distortion
if ($GRAMMAR_TYPE eq "phrasal") {
  $GLUE_GRAMMAR_FILE = get_absolute_path("$JOSHUA/data/glue-grammar.itg");
}

# use this default unless it's already been defined by a command-line argument
$THRAX_CONF_FILE = "$JOSHUA/scripts/training/templates/thrax-$GRAMMAR_TYPE.conf" unless defined $THRAX_CONF_FILE;

mkdir $RUNDIR unless -d $RUNDIR;
chdir($RUNDIR);

if (defined $README) {
  open DESC, ">README" or die "can't write README file";
  print DESC $README;
  print DESC $/;
  close DESC;
}

# default values -- these are overridden if the full script is run
# (after tokenization and normalization)
my (%TRAIN,%TUNE,%TEST);
if (@CORPORA) {
  $TRAIN{prefix} = $CORPORA[0];
  $TRAIN{source} = "$CORPORA[0].$SOURCE";
  $TRAIN{target} = "$CORPORA[0].$TARGET";
}

# set the location of the parsed corpus if that was defined
if (defined $PARSED_CORPUS) {
  $TRAIN{parsed} = get_absolute_path($PARSED_CORPUS);
}

if ($TUNE) {
  $TUNE{source} = "$TUNE.$SOURCE";
  $TUNE{target} = "$TUNE.$TARGET";

  if (! -e "$TUNE{source}") {
    print "* FATAL: couldn't find tune source file at '$TUNE{source}'\n";
    exit;
  }
}

if ($TEST) {
  $TEST{source} = "$TEST.$SOURCE";
  $TEST{target} = "$TEST.$TARGET";

  if (! -e "$TEST{source}") {
    print "* FATAL: couldn't find test source file at '$TEST{source}'\n";
    exit;
  }
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
    ;

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
my %PREPPED = (
  TRAIN => 0,
  TUNE => 0,
  TEST => 0
		);


if ($DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("train",\@CORPORA,$MAXLEN);

  # used for parsing
  $TRAIN{mixedcase} = "$DATA_DIRS{train}/$prefixes->{shortened}.$TARGET.gz";

  $TRAIN{prefix} = "$DATA_DIRS{train}/corpus";
  $TRAIN{source} = "$DATA_DIRS{train}/corpus.$SOURCE";
  $TRAIN{target} = "$DATA_DIRS{train}/corpus.$TARGET";
  $PREPPED{TRAIN} = 1;
}

# prepare the tuning and development data
if (defined $TUNE and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("tune",[$TUNE],$MAXLEN_TUNE);
  $TUNE{source} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TUNE} = 1;
}

if (defined $TEST and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("test",[$TEST],$MAXLEN_TEST);
  $TEST{source} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$SOURCE";
  $TEST{target} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TEST} = 1;
}

maybe_quit("FIRST");

## SUBSAMPLE #########################################################

SUBSAMPLE:
    ;

# subsample
		if ($DO_SUBSAMPLE) {
			mkdir("$DATA_DIRS{train}/subsampled") unless -d "$DATA_DIRS{train}/subsampled";

			$cachepipe->cmd("subsample-manifest",
											"echo corpus > $DATA_DIRS{train}/subsampled/manifest",
											"$DATA_DIRS{train}/subsampled/manifest");

			$cachepipe->cmd("subsample-testdata",
											"cat $TUNE{source} $TEST{source} > $DATA_DIRS{train}/subsampled/test-data",
											$TUNE{source},
											$TEST{source},
											"$DATA_DIRS{train}/subsampled/test-data");

			$cachepipe->cmd("subsample",
											"java -Xmx4g -Dfile.encoding=utf8 -cp $JOSHUA/bin:$JOSHUA/lib/commons-cli-2.0-SNAPSHOT.jar joshua.subsample.Subsampler -e $TARGET -f $SOURCE -epath $DATA_DIRS{train}/ -fpath $DATA_DIRS{train}/ -output $DATA_DIRS{train}/subsampled/subsampled.$MAXLEN -ratio 1.04 -test $DATA_DIRS{train}/subsampled/test-data -training $DATA_DIRS{train}/subsampled/manifest",
											"$DATA_DIRS{train}/subsampled/manifest",
											"$DATA_DIRS{train}/subsampled/test-data",
											$TRAIN{source},
											$TRAIN{target},
											"$DATA_DIRS{train}/subsampled/subsampled.$MAXLEN.$TARGET",
											"$DATA_DIRS{train}/subsampled/subsampled.$MAXLEN.$SOURCE");

			# rewrite the symlinks to point to the subsampled corpus
			foreach my $lang ($TARGET,$SOURCE) {
				system("ln -sf subsampled/subsampled.$MAXLEN.$lang $DATA_DIRS{train}/corpus.$lang");
			}
}

maybe_quit("SUBSAMPLE");


## ALIGN #############################################################

ALIGN:
    ;

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

  # We process the data in chunks which by default are 1,000,000 sentence pairs.  So first split up
  # the data into those chunks.
  system("mkdir","-p","$DATA_DIRS{train}/splits") unless -d "$DATA_DIRS{train}/splits";

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
			open CHUNK_SOURCE, ">", "$DATA_DIRS{train}/splits/corpus.$SOURCE.$chunk" or die;
			open CHUNK_TARGET, ">", "$DATA_DIRS{train}/splits/corpus.$TARGET.$chunk" or die;

			$lastchunk = $chunk;
		}

		print CHUNK_SOURCE $source;
		print CHUNK_TARGET $target;
  }
  close CHUNK_SOURCE;
  close CHUNK_TARGET;

  close SOURCE;
  close TARGET;

  # my $max_aligner_threads = $NUM_THREADS;
  # if ($ALIGNER eq "giza" and $max_aligner_threads > 1) {
  #   $max_aligner_threads /= 2;
  # }

  # # With multi-threading, we can use a pool to set up concurrent GIZA jobs on the chunks.
  #
  # TODO: implement this.  There appears to be a problem with calling system() in threads.
  #
  # my $pool = new Thread::Pool(Min => 1, Max => $max_aligner_threads);

  system("mkdir alignments") unless -d "alignments";

  if ($lastchunk == 0 || $NUM_JOBS == 1) {
    system("seq 0 $lastchunk | $SCRIPTDIR/training/paralign.pl -aligner $ALIGNER -num_threads $NUM_THREADS -giza_merge $GIZA_MERGE -aligner_mem $ALIGNER_MEM -source $SOURCE -target $TARGET -giza_trainer \"$GIZA_TRAINER\" -train_dir \"$DATA_DIRS{train}\" > alignments/run.log");
  } else {
    system("seq 0 $lastchunk | $JOSHUA/scripts/training/parallelize/parallelize.pl --err err --jobs $NUM_JOBS --qsub-args \"$QSUB_ALIGN_ARGS\" -p $ALIGNER_MEM -- $SCRIPTDIR/training/paralign.pl -aligner $ALIGNER -num_threads $NUM_THREADS -giza_merge $GIZA_MERGE -aligner_mem $ALIGNER_MEM -source $SOURCE -target $TARGET -giza_trainer \"$GIZA_TRAINER\" -train_dir \"$DATA_DIRS{train}\" > alignments/run.log");
  }

  my @aligned_files;
  if ($ALIGNER eq "giza") {
    @aligned_files = map { "alignments/$_/model/aligned.$GIZA_MERGE" } (0..$lastchunk);
  } elsif ($ALIGNER eq "berkeley") {
    @aligned_files = map { "alignments/$_/training.align" } (0..$lastchunk);
  } elsif ($ALIGNER eq "jacana") {
    @aligned_files = map { "alignments/$_/training.align" } (0..$lastchunk);
  }
	my $aligned_file_list = join(" ", @aligned_files);

  # wait for all the threads to finish
  # $pool->join();

	# combine the alignments
	$cachepipe->cmd("aligner-combine",
									"cat $aligned_file_list > alignments/training.align",
									$aligned_files[-1],
									"alignments/training.align");

  # at the end, all the files are concatenated into a single alignment file parallel to the input
  # corpora
  $ALIGNMENT = "alignments/training.align";
}

maybe_quit("ALIGN");


## PARSE #############################################################

PARSE:
    ;

# Parsing only happens for SAMT grammars.

if ($FIRST_STEP eq "PARSE" and ($GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "phrasal")) {
  print STDERR "* FATAL: parsing doesn't apply to hiero grammars; You need to add '--type samt'\n";
  exit;
}

if ($GRAMMAR_TYPE eq "samt" || $GRAMMAR_TYPE eq "ghkm") {

  # If the user passed in the already-parsed corpus, use that (after copying it into place)
  if (defined $TRAIN{parsed} && -e $TRAIN{parsed}) {
    # copy and adjust the location of the file to its canonical location
    system("cp $TRAIN{parsed} $DATA_DIRS{train}/corpus.parsed.$TARGET");
    $TRAIN{parsed} = "$DATA_DIRS{train}/corpus.parsed.$TARGET";
  } else {

    system("mkdir -p $DATA_DIRS{train}") unless -e $DATA_DIRS{train};

    $cachepipe->cmd("build-vocab",
                    "cat $TRAIN{target} | $SCRIPTDIR/training/build-vocab.pl > $DATA_DIRS{train}/vocab.$TARGET",
                    $TRAIN{target},
                    "$DATA_DIRS{train}/vocab.$TARGET");

    my $file_to_parse = (exists $TRAIN{mixedcase}) ? $TRAIN{mixedcase} : $TRAIN{target};

    if ($NUM_JOBS > 1) {
      # the black-box parallelizer model doesn't work with multiple
      # threads, so we're always spawning single-threaded instances here

      # open PARSE, ">parse.sh" or die;
      # print PARSE "cat $TRAIN{target} | $JOSHUA/scripts/training/parallelize/parallelize.pl --jobs $NUM_JOBS --qsub-args \"$QSUB_ARGS\" -- java -d64 -Xmx${PARSER_MEM} -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr -nThreads 1 | sed 's/^\(/\(TOP/' | tee $DATA_DIRS{train}/corpus.$TARGET.parsed.mc | perl -pi -e 's/(\\S+)\\)/lc(\$1).\")\"/ge' | tee $DATA_DIRS{train}/corpus.$TARGET.parsed | perl $SCRIPTDIR/training/add-OOVs.pl $DATA_DIRS{train}/vocab.$TARGET > $DATA_DIRS{train}/corpus.parsed.$TARGET\n";
      # close PARSE;
      # chmod 0755, "parse.sh";
      # $cachepipe->cmd("parse",
      #         "setsid ./parse.sh",
      #         "$TRAIN{target}",
      #         "$DATA_DIRS{train}/corpus.parsed.$TARGET");

      $cachepipe->cmd("parse",
                      "$CAT $file_to_parse | $JOSHUA/scripts/training/parallelize/parallelize.pl --jobs $NUM_JOBS --qsub-args \"$QSUB_ARGS\" -p 8g -- java -d64 -Xmx${PARSER_MEM} -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr -nThreads 1 | sed 's/^(())\$//; s/^(/(TOP/' | perl $SCRIPTDIR/training/add-OOVs.pl $DATA_DIRS{train}/vocab.$TARGET | tee $DATA_DIRS{train}/corpus.$TARGET.Parsed | $SCRIPTDIR/training/lowercase-leaves.pl > $DATA_DIRS{train}/corpus.parsed.$TARGET",
                      "$TRAIN{target}",
                      "$DATA_DIRS{train}/corpus.parsed.$TARGET");
    } else {
      # Multi-threading in the Berkeley parser is broken, so we use a black-box parallelizer on top
      # of it.
      $cachepipe->cmd("parse",
                      "$CAT $file_to_parse | $JOSHUA/scripts/training/parallelize/parallelize.pl --jobs $NUM_THREADS --use-fork -- java -d64 -Xmx${PARSER_MEM} -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr -nThreads 1 | sed 's/^(())\$//; s/^(/(TOP/' | perl $SCRIPTDIR/training/add-OOVs.pl $DATA_DIRS{train}/vocab.$TARGET | tee $DATA_DIRS{train}/corpus.$TARGET.Parsed | $SCRIPTDIR/training/lowercase-leaves.pl > $DATA_DIRS{train}/corpus.parsed.$TARGET",
                      "$TRAIN{target}",
                      "$DATA_DIRS{train}/corpus.parsed.$TARGET");
    }

    $TRAIN{parsed} = "$DATA_DIRS{train}/corpus.parsed.$TARGET";
  }
}

maybe_quit("PARSE");

## THRAX #############################################################

GRAMMAR:
    ;
THRAX:
    ;

system("mkdir -p $DATA_DIRS{train}") unless -d $DATA_DIRS{train};

if ($GRAMMAR_TYPE eq "samt" || $GRAMMAR_TYPE eq "ghkm") {

  # if we jumped right here, $TRAIN{target} should be parsed
  if (exists $TRAIN{parsed}) {
		# parsing step happened in-script or a parsed corpus was passed in explicitly, all is well

  } elsif (already_parsed($TRAIN{target})) {
		# skipped straight to this step, passing a parsed corpus

		$TRAIN{parsed} = "$DATA_DIRS{train}/corpus.parsed.$TARGET";
		
		$cachepipe->cmd("cp-train-$TARGET",
										"cp $TRAIN{target} $TRAIN{parsed}",
										$TRAIN{target}, 
										$TRAIN{parsed});

		$TRAIN{target} = "$DATA_DIRS{train}/corpus.$TARGET";

		# now extract the leaves of the parsed corpus
		$cachepipe->cmd("extract-leaves",
										"cat $TRAIN{parsed} | perl -pe 's/\\(.*?(\\S\+)\\)\+?/\$1/g' | perl -pe 's/\\)//g' > $TRAIN{target}",
										$TRAIN{parsed},
										$TRAIN{target});

		if ($TRAIN{source} ne "$DATA_DIRS{train}/corpus.$SOURCE") {
			$cachepipe->cmd("cp-train-$SOURCE",
											"cp $TRAIN{source} $DATA_DIRS{train}/corpus.$SOURCE",
											$TRAIN{source}, "$DATA_DIRS{train}/corpus.$SOURCE");
			$TRAIN{source} = "$DATA_DIRS{train}/corpus.$SOURCE";
		}

  } else {
		print "* FATAL: You requested to build an SAMT grammar, but provided an\n";
		print "  unparsed corpus.  Please re-run the pipeline and begin no later\n";
		print "  than the PARSE step (--first-step PARSE), or pass in a parsed corpus\n";
		print "  using --parsed-corpus CORPUS.\n";
		exit 1;
  }
	
}

# we may have skipped directly to this step, in which case we need to
# ensure an alignment was provided
if (! defined $ALIGNMENT) {
  print "* FATAL: no alignment file specified\n";
  exit(1);
}

# If the grammar file wasn't specified
if (! defined $GRAMMAR_FILE) {

  my $target_file = ($GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "phrasal") ? $TRAIN{target} : $TRAIN{parsed};

  if ($GRAMMAR_TYPE eq "ghkm") {
    if ($GHKM_EXTRACTOR eq "galley") {
      $cachepipe->cmd("ghkm-extract",
                      "java -Xmx4g -Xms4g -cp $JOSHUA/lib/ghkm-modified.jar:$JOSHUA/lib/fastutil.jar -XX:+UseCompressedOops edu.stanford.nlp.mt.syntax.ghkm.RuleExtractor -fCorpus $TRAIN{source} -eParsedCorpus $target_file -align $ALIGNMENT -threads $NUM_THREADS -joshuaFormat true -maxCompositions 1 -reversedAlignment false | $SCRIPTDIR/support/splittabs.pl ghkm-mapping.gz grammar.gz",
                      $ALIGNMENT,
                      "grammar.gz");
    } elsif ($GHKM_EXTRACTOR eq "moses") {
      # XML-ize, also replacing unary chains with OOV at the bottom by removing their unary parents
      $cachepipe->cmd("ghkm-moses-xmlize",
                      "cat $target_file | perl -pe 's/\\(\\S+ \\(OOV (.*?)\\)\\)/(OOV \$1)/g' | $MOSES/scripts/training/wrappers/berkeleyparsed2mosesxml.perl > $DATA_DIRS{train}/corpus.xml",
                      # "cat $target_file | perl -pe 's/\\(\\S+ \\(OOV (.*?)\\)\\)/(OOV \$1)/g' > $DATA_DIRS{train}/corpus.ptb",
                      $target_file,
                      "$DATA_DIRS{train}/corpus.xml");

      if (! -e "$DATA_DIRS{train}/corpus.$SOURCE") {
        system("ln -sf $TRAIN{source} $DATA_DIRS{train}/corpus.$SOURCE");
      }

      if ($ALIGNMENT ne "alignments/training.align") {
        system("mkdir alignments") unless -d "alignments";
        system("ln -sf $ALIGNMENT alignments/training.align");
        $ALIGNMENT = "alignments/training.align";
      }

      system("mkdir model");
      $cachepipe->cmd("ghkm-moses-extract",
                      "$MOSES/scripts/training/train-model.perl --first-step 4 --last-step 6 --corpus $DATA_DIRS{train}/corpus --ghkm --f $SOURCE --e xml --alignment-file alignments/training --alignment align --target-syntax --cores $NUM_THREADS --pcfg --alt-direct-rule-score-1 --ghkm-tree-fragments --glue-grammar --glue-grammar-file glue-grammar.ghkm --extract-options \"$EXTRACT_OPTIONS --UnknownWordLabel oov-labels.txt\"",
                      "$DATA_DIRS{train}/corpus.xml",
                      "glue-grammar.ghkm",
                      "model/rule-table.gz");

      open LABELS, "oov-labels.txt";
      chomp(my @labels = <LABELS>);
      close LABELS;
      my $oov_list = "\"" . join(" ", @labels) . "\"";
      $JOSHUA_ARGS .= " -oov-list $oov_list";

      $cachepipe->cmd("ghkm-moses-convert",
                      "gzip -cd model/rule-table.gz | /home/hltcoe/mpost/code/joshua/scripts/support/moses2joshua_grammar.pl | gzip -9n > grammar.gz",
                      "model/rule-table.gz",
                      "grammar.gz");

    } else {
      print STDERR "* FATAL: no such GHKM extractor '$GHKM_EXTRACTOR'\n";
      exit(1);
    }
  } elsif (! -e "grammar.gz" && ! -z "grammar.gz") {

    # Since this is an expensive step, we short-circuit it if the grammar file is present.  I'm not
    # sure that this is the right behavior.

    # create the input file
    $cachepipe->cmd("thrax-input-file",
                    "paste $TRAIN{source} $target_file $ALIGNMENT | perl -pe 's/\\t/ ||| /g' | grep -v '()' | grep -v '||| \\+\$' > $DATA_DIRS{train}/thrax-input-file",
                    $TRAIN{source}, $target_file, $ALIGNMENT,
                    "$DATA_DIRS{train}/thrax-input-file");


    # Rollout the hadoop cluster if needed.  This causes $HADOOP to be defined (pointing to the
    # unrolled directory).
    start_hadoop_cluster() unless defined $HADOOP;

    # put the hadoop files in place
    my $THRAXDIR;
    my $thrax_input;
    if ($HADOOP eq "hadoop") {
      $THRAXDIR = "thrax";

      $thrax_input = "$DATA_DIRS{train}/thrax-input-file"

    } else {
      $THRAXDIR = "pipeline-$SOURCE-$TARGET-$GRAMMAR_TYPE-$RUNDIR";
      $THRAXDIR =~ s#/#_#g;

      $cachepipe->cmd("thrax-prep",
                      "$HADOOP/bin/hadoop fs -rmr $THRAXDIR; $HADOOP/bin/hadoop fs -mkdir $THRAXDIR; $HADOOP/bin/hadoop fs -put $DATA_DIRS{train}/thrax-input-file $THRAXDIR/input-file",
                      "$DATA_DIRS{train}/thrax-input-file", 
                      "grammar.gz");

      $thrax_input = "$THRAXDIR/input-file";
    }

    # copy the thrax config file
    my $thrax_file = "thrax-$GRAMMAR_TYPE.conf";
    system("grep -v ^input-file $THRAX_CONF_FILE > $thrax_file.tmp");
    system("echo input-file $thrax_input >> $thrax_file.tmp");
    system("mv $thrax_file.tmp $thrax_file");

    $cachepipe->cmd("thrax-run",
                    "$HADOOP/bin/hadoop jar $THRAX/bin/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' $thrax_file $THRAXDIR > thrax.log 2>&1; rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $THRAXDIR/final/ grammar.gz; $HADOOP/bin/hadoop fs -rmr $THRAXDIR",
                    "$DATA_DIRS{train}/thrax-input-file",
                    $thrax_file,
                    "grammar.gz");
#perl -pi -e 's/\.?0+\b//g' grammar; 

    stop_hadoop_cluster() if $HADOOP eq "hadoop";

    # cache the thrax-prep step, which depends on grammar.gz
    if ($HADOOP ne "hadoop") {
      $cachepipe->cmd("thrax-prep", "--cache-only");
    }

    # clean up
    # TODO: clean up real hadoop clusters too
    if ($HADOOP eq "hadoop") {
      system("rm -rf $THRAXDIR hadoop hadoop-0.20.2");
    }
  }

  # set the grammar file
  $GRAMMAR_FILE = "grammar.gz";
}

maybe_quit("THRAX");
maybe_quit("GRAMMAR");

## TUNING ##############################################################
TUNE:
    ;

# prep the tuning data, unless already prepped
if (! $PREPPED{TUNE} and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("tune",[$TUNE],$MAXLEN_TUNE);
  $TUNE{source} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TUNE} = 1;
}

sub compile_lm($) {
  my $lmfile = shift;
  if ($LM_TYPE eq "kenlm") {
    my $kenlm_file = basename($lmfile, ".gz") . ".kenlm";
    $cachepipe->cmd("compile-kenlm",
                    "$JOSHUA/src/joshua/decoder/ff/lm/kenlm/build_binary $lmfile $kenlm_file",
                    $lmfile, $kenlm_file);
    return $kenlm_file;

  } elsif ($LM_TYPE eq "berkeleylm") {
    my $berkeleylm_file = basename($lmfile, ".gz") . ".berkeleylm";
    $cachepipe->cmd("compile-berkeleylm",
                    "java -cp $JOSHUA/lib/berkeleylm.jar -server -mx$BUILDLM_MEM edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa $lmfile $berkeleylm_file",
                    $lmfile, $berkeleylm_file);
    return $berkeleylm_file;

  } else {
    print "* FATAL: trying to compile an LM to neither kenlm nor berkeleylm.";
    exit 2;
  }
}

# Build the language model if needed
if ($DO_BUILD_LM_FROM_CORPUS) {

  # make sure the training data is prepped
  if (! $PREPPED{TRAIN} and $DO_PREPARE_CORPORA) {
		my $prefixes = prepare_data("train",\@CORPORA,$MAXLEN);

		$TRAIN{prefix} = "$DATA_DIRS{train}/corpus";
		foreach my $lang ($SOURCE,$TARGET) {
			system("ln -sf $prefixes->{lowercased}.$lang $DATA_DIRS{train}/corpus.$lang");
		}
		$TRAIN{source} = "$DATA_DIRS{train}/corpus.$SOURCE";
		$TRAIN{target} = "$DATA_DIRS{train}/corpus.$TARGET";
		$PREPPED{TRAIN} = 1;
  }

  if (! -e $TRAIN{target}) {
		print "* FATAL: I need a training corpus to build the language model from (--corpus)\n";
		exit(1);
  }

  my $lmfile = "lm.gz";

  # sort and uniq the training data
  $cachepipe->cmd("lm-sort-uniq",
                  "$CAT $TRAIN{target} | sort -u -T $TMPDIR -S $BUILDLM_MEM | gzip -9n > $TRAIN{target}.uniq",
                  $TRAIN{target},
                  "$TRAIN{target}.uniq");

  if ($LM_GEN eq "srilm") {
		my $smoothing = ($WITTEN_BELL) ? "-wbdiscount" : "-kndiscount";
		$cachepipe->cmd("srilm",
										"$SRILM -order $LM_ORDER -interpolate $smoothing -unk -gt3min 1 -gt4min 1 -gt5min 1 -text $TRAIN{target}.uniq -lm lm.gz",
                    "$TRAIN{target}.uniq",
										$lmfile);
  } elsif ($LM_GEN eq "berkeleylm") {
		$cachepipe->cmd("berkeleylm",
										"java -ea -mx$BUILDLM_MEM -server -cp $JOSHUA/lib/berkeleylm.jar edu.berkeley.nlp.lm.io.MakeKneserNeyArpaFromText $LM_ORDER lm.gz $TRAIN{target}.uniq",
                    "$TRAIN{target}.uniq",
										$lmfile);
  } else {
    # Make sure it exists (doesn't build for OS X)
    if (! -e "$JOSHUA/bin/lmplz") {
      print "* FATAL: $JOSHUA/bin/lmplz (for building LMs) does not exist.\n";
      print "  If you are on OS X, you need to use either SRILM (recommended) or BerkeleyLM,\n";
      print "  triggered with '--lm-gen srilm' or '--lm-gen berkeleylm'. If you are on Linux,\n";
      print "  you should run \"ant -f \$JOSHUA/build.xml kenlm\".\n";
      exit 1;
    }

    # Needs to be capitalized
    my $mem = uc $BUILDLM_MEM;
    $cachepipe->cmd("kenlm",
                    "$JOSHUA/bin/lmplz -o $LM_ORDER -T $TMPDIR -S $mem --verbose_header --text $TRAIN{target}.uniq | gzip -9n > lm.gz",
                    "$TRAIN{target}.uniq",
                    $lmfile);
  }

  if ((! $MERGE_LMS) && ($LM_TYPE eq "kenlm" || $LM_TYPE eq "berkeleylm")) {
    push (@LMFILES, get_absolute_path(compile_lm $lmfile, $RUNDIR));
  } else {
    push (@LMFILES, get_absolute_path($lmfile, $RUNDIR));
  }
}

if ($MERGE_LMS) {
  # Merge @LMFILES.
  my $merged_lm = "lm-merged.gz";
  print "@LMFILES";
  $cachepipe->cmd("merge-lms",
                  "$JOSHUA/scripts/support/merge_lms.py "
                    . "@LMFILES "
                    . "$TUNE{target} "
                    . "lm-merged.gz "
                    . "--temp-dir data/merge_lms ",
                  @LMFILES,
                  $merged_lm);

  # Empty out @LMFILES.
  @LMFILES = ();

  # Compile merged LM
  if ($LM_TYPE eq "kenlm" || $LM_TYPE eq "berkeleylm") {
    push (@LMFILES, get_absolute_path(compile_lm $merged_lm, $RUNDIR));

  } else {
    push (@LMFILES, get_absolute_path($merged_lm, $RUNDIR));
  }
}

system("mkdir -p $DATA_DIRS{tune}") unless -d $DATA_DIRS{tune};

# figure out how many references there are
my $numrefs = get_numrefs($TUNE{target});

# make sure the dev source exist
if (! -e $TUNE{source}) {
  print STDERR "* FATAL: couldn't fine tuning source file '$TUNE{source}'\n";
  exit 1;
}
if ($numrefs > 1) {
  for my $i (0..$numrefs-1) {
		if (! -e "$TUNE{target}.$i") {
			print STDERR "* FATAL: couldn't find tuning reference file '$TUNE{target}.$i'\n";
			exit 1;
		}
  }
} else {
  if (! -e $TUNE{target}) {
		print STDERR "* FATAL: couldn't find tuning reference file '$TUNE{target}'\n";
		exit 1;
  }
}


# Filter the tuning grammar if it was requested (yes by default) and a tuned grammar was not passed
# in explicitly.
my $TUNE_GRAMMAR = (defined $TUNE_GRAMMAR_FILE)
		? $TUNE_GRAMMAR_FILE
		: $GRAMMAR_FILE;

if ($DO_FILTER_TM and ! $DOING_LATTICES and ! defined $TUNE_GRAMMAR_FILE) {
  $TUNE_GRAMMAR = "$DATA_DIRS{tune}/grammar.filtered.gz";

  $cachepipe->cmd("filter-tune",
									"$CAT $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter $FILTERING -v $TUNE{source} | $SCRIPTDIR/training/filter-rules.pl -bus$SCOPE | gzip -9n > $TUNE_GRAMMAR",
									$GRAMMAR_FILE,
									$TUNE{source},
									$TUNE_GRAMMAR);
}

# Pack the grammar, if requested (yes by default). This must be done after the glue grammar is
# created, since we don't have a script (yet) to dump the rules from a packed grammar, which
# information we need to create the glue grammar.
if ($DO_PACK_GRAMMARS && ! is_packed($TUNE_GRAMMAR)) {
  my $packed_dir = "$DATA_DIRS{tune}/grammar.packed";

  $cachepipe->cmd("pack-tune",
                  "$SCRIPTDIR/support/grammar-packer.pl -T $TMPDIR -m $PACKER_MEM $TUNE_GRAMMAR $packed_dir",
                  $TUNE_GRAMMAR,
                  "$packed_dir/vocabulary",
                  "$packed_dir/encoding",
                  "$packed_dir/slice_00000.source");

  # $TUNE_GRAMMAR_FILE, which previously held an optional command-line argument of a pre-filtered
  # tuning grammar, is now used to record the text-based grammar, which is needed later for
  # different things.
  $TUNE_GRAMMAR_FILE = $TUNE_GRAMMAR;

  # The actual grammar used for decoding is the packed directory.
  $TUNE_GRAMMAR = $packed_dir;
}

# Create the glue grammars. This is done by looking at all the symbols in the grammar file and
# creating all the needed rules.
if (! defined $GLUE_GRAMMAR_FILE) {
  $cachepipe->cmd("glue-tune",
                  "java -Xmx2g -cp $JOSHUA/lib/*:$THRAX/bin/thrax.jar edu.jhu.thrax.util.CreateGlueGrammar $TUNE_GRAMMAR > $DATA_DIRS{tune}/grammar.glue",
                  $TUNE_GRAMMAR_FILE,
                  "$DATA_DIRS{tune}/grammar.glue");
  $GLUE_GRAMMAR_FILE = "$DATA_DIRS{tune}/grammar.glue";
} else {
  # just create a symlink to it
  my $filename = $DATA_DIRS{tune} . "/" . basename($GLUE_GRAMMAR_FILE);
  system("ln -sf $GLUE_GRAMMAR_FILE $filename");
}

# For each language model, we need to create an entry in the Joshua
# config file and in ZMERT's params.txt file.  We use %lm_strings to
# build the corresponding string substitutions
my (@configstrings, @lmweightstrings, @lmparamstrings);
for my $i (0..$#LMFILES) {
  my $lmfile = $LMFILES[$i];

  my $configstring = "lm = $LM_TYPE $LM_ORDER $LM_STATE_MINIMIZATION false 100 $lmfile";
  push (@configstrings, $configstring);

  my $weightstring = "lm_$i 1.0";
  push (@lmweightstrings, $weightstring);

  my $lmparamstring = "lm_$i        |||     1.000000 Opt     0.1     +Inf    +0.5    +1.5";
  push (@lmparamstrings, $lmparamstring);
}

my $lmlines   = join($/, @configstrings);
my $lmweights = join($/, @lmweightstrings);
my $lmparams  = join($/, @lmparamstrings);

my (@tmparamstrings, @tmweightstrings);
open CONFIG, $TUNEFILES{'joshua.config'} or die;
while (my $line = <CONFIG>) {
  if ($line =~ /^tm\s*=/) {
    $line =~ s/\s+$//;
    my (undef,$grammarline) = split(/\s*=\s*/, $line);
    my (undef,$owner,$span,$grammar) = split(' ', $grammarline);

    if ($grammar =~ /<GRAMMAR_FILE>/ or $grammar =~ /<GLUE_GRAMMAR>/) {
      
      my $grammar_file = ($grammar =~ /<GRAMMAR_FILE>/) ? $TUNE_GRAMMAR : $GLUE_GRAMMAR_FILE;

      # Add the weights for the tuning grammar.
      my @features = get_features($grammar_file);
      foreach my $feature (@features) {
        if ($feature =~ /^\d+$/) {  # dense feature
          push (@tmparamstrings, "tm_${owner}_$feature ||| 1.0 Opt -Inf +Inf -1 +1");
          push (@tmweightstrings, "tm_${owner}_$feature 1.0");
        } else {  # sparse feature
          push (@tmparamstrings, "$feature ||| 0.0 Opt -Inf +Inf -1 +1");
          push (@tmweightstrings, "$feature 0.0");
        }
      }

    } else {
      # Add weights for any pre-supplied grammars.

      my @features = get_features($grammar);
      foreach my $feature (@features) {
        if ($feature =~ /^\d+$/) {  # dense feature
          push (@tmparamstrings, "tm_${owner}_$feature ||| 1.0 Opt -Inf +Inf -1 +1");
          push (@tmweightstrings, "tm_${owner}_$feature 1.0");
        } else {  # sparse feature
          push (@tmparamstrings, "$feature ||| 0.0 Opt -Inf +Inf -1 +1");
          push (@tmweightstrings, "$feature 0.0");
        }
      }
		}
	}
}
close CONFIG;

my $tmparams = join($/, @tmparamstrings);
my $tmweights = join($/, @tmweightstrings);

my $latticeparam = ($DOING_LATTICES == 1) 
		? "SourcePath ||| 1.0 Opt -Inf +Inf -1 +1"
		: "";
my $latticeweight = ($DOING_LATTICES == 1)
		? "SourcePath 1.0"
		: "";

my @feature_functions;
if ($DOING_LATTICES) {
  push(@feature_functions, "feature_function = SourcePath");
}
my $feature_functions = join("\n", @feature_functions);

for my $run (1..$OPTIMIZER_RUNS) {
  my $tunedir = (defined $NAME) ? "tune/$NAME/$run" : "tune/$run";
  system("mkdir -p $tunedir") unless -d $tunedir;

  foreach my $key (keys %TUNEFILES) {
		my $file = $TUNEFILES{$key};
		open FROM, $file or die "can't find file '$file'";
		open TO, ">$tunedir/$key" or die "can't write to file '$tunedir/$key'";
		while (<FROM>) {
			s/<INPUT>/$TUNE{source}/g;
			s/<SOURCE>/$SOURCE/g;
			s/<RUNDIR>/$RUNDIR/g;
			s/<TARGET>/$TARGET/g;
			s/<LMLINES>/$lmlines/g;
			s/<LMWEIGHTS>/$lmweights/g;
			s/<TMWEIGHTS>/$tmweights/g;
			s/<LMPARAMS>/$lmparams/g;
			s/<TMPARAMS>/$tmparams/g;
      s/<FEATURE_FUNCTIONS>/$feature_functions/g;
			s/<LATTICEWEIGHT>/$latticeweight/g;
			s/<LATTICEPARAM>/$latticeparam/g;
			s/<LMFILE>/$LMFILES[0]/g;
			s/<LMTYPE>/$LM_TYPE/g;
			s/<MEM>/$JOSHUA_MEM/g;
			s/<GRAMMAR_TYPE>/$GRAMMAR_TYPE/g;
			s/<GRAMMAR_FILE>/$TUNE_GRAMMAR/g;
			s/<GLUE_GRAMMAR>/$GLUE_GRAMMAR_FILE/g;
			s/<MAXSPAN>/$MAXSPAN/g;
			s/<OOV>/$OOV/g;
			s/<NUMJOBS>/$NUM_JOBS/g;
			s/<NUMTHREADS>/$NUM_THREADS/g;
			s/<QSUB_ARGS>/$QSUB_ARGS/g;
			s/<OUTPUT>/$tunedir\/tune.output.nbest/g;
			s/<REF>/$TUNE{target}/g;
			s/<JOSHUA>/$JOSHUA/g;
			s/<JOSHUA_ARGS>/$JOSHUA_ARGS/g;
			s/<NUMREFS>/$numrefs/g;
			s/<CONFIG>/$tunedir\/joshua.config/g;
			s/<LOG>/$tunedir\/joshua.log/g;
			s/<TUNEDIR>/$tunedir/g;
			s/<MERTDIR>/$tunedir/g;   # for backwards compatibility
			s/use_sent_specific_tm=.*/use_sent_specific_tm=0/g;
			print TO;
		}
		close(FROM);
		close(TO);
  }
  chmod(0755,"$tunedir/decoder_command");

  # tune
  if ($TUNER eq "mert") {
		$cachepipe->cmd("mert-$run",
										"java -d64 -Xmx2g -cp $JOSHUA/class joshua.zmert.ZMERT -maxMem 4500 $tunedir/mert.config > $tunedir/mert.log 2>&1",
										$TUNE_GRAMMAR_FILE,
										"$tunedir/joshua.config.ZMERT.final",
										"$tunedir/decoder_command",
										"$tunedir/mert.config",
										"$tunedir/params.txt");
		system("ln -sf joshua.config.ZMERT.final $tunedir/joshua.config.final");
  } elsif ($TUNER eq "pro") {
		$cachepipe->cmd("pro-$run",
										"java -d64 -Xmx2g -cp $JOSHUA/class joshua.pro.PRO -maxMem 4500 $tunedir/pro.config > $tunedir/pro.log 2>&1",
										$TUNE_GRAMMAR_FILE,
										"$tunedir/joshua.config.PRO.final",
										"$tunedir/decoder_command",
										"$tunedir/pro.config",
										"$tunedir/params.txt");
		system("ln -sf joshua.config.PRO.final $tunedir/joshua.config.final");
  } elsif ($TUNER eq "mira") {
    my $refs_path = $TUNE{target};
    $refs_path .= "." if (get_numrefs($TUNE{target}) > 1);

    my $rescore_str = ($RESCORE_FOREST == 1) ? "--rescore-forest" : "--no-rescore-forest";
    
    my $extra_args = $JOSHUA_ARGS;
    $extra_args =~ s/"/\\"/g;
    $cachepipe->cmd("mira-$run",
                    "$SCRIPTDIR/training/mira/run-mira.pl --input $TUNE{source} --refs $refs_path --config $tunedir/joshua.config --decoder $JOSHUA/bin/decoder --mertdir $MOSES/bin --rootdir $MOSES/scripts --batch-mira --working-dir $tunedir --maximum-iterations $MIRA_ITERATIONS --return-best-dev --nbest $NBEST --decoder-flags \"-m $JOSHUA_MEM -threads $NUM_THREADS $extra_args\" $rescore_str > $tunedir/mira.log 2>&1",
                    $TUNE_GRAMMAR_FILE,
                    $TUNE{source},
                    "$tunedir/joshua.config.final");
  }

  # Go to the next tuning run if tuning is the last step.
  if ($LAST_STEP eq "TUNE") {
    next;
  }


# prepare the testing data
  if (! $PREPPED{TEST} and $DO_PREPARE_CORPORA) {
    my $prefixes = prepare_data("test",[$TEST],$MAXLEN_TEST);
    $TEST{source} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$SOURCE";
    $TEST{target} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$TARGET";
    $PREPPED{TEST} = 1;
  }

# filter the test grammar
  system("mkdir -p $DATA_DIRS{test}") unless -d $DATA_DIRS{test};
  my $TEST_GRAMMAR;
  if ($TEST_GRAMMAR_FILE) {
    # if a specific test grammar was specified, use that (no filtering)
    $TEST_GRAMMAR = $TEST_GRAMMAR_FILE;
  } else {
    # otherwise, use the main grammar, and filter it if requested
    $TEST_GRAMMAR = $GRAMMAR_FILE;
    
    if ($DO_FILTER_TM and ! $DOING_LATTICES) {
      $TEST_GRAMMAR = "$DATA_DIRS{test}/grammar.filtered.gz";

      $cachepipe->cmd("filter-test",
                      "$SCRIPTDIR/training/scat $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter $FILTERING -v $TEST{source} | $SCRIPTDIR/training/filter-rules.pl -bus$SCOPE | gzip -9n > $TEST_GRAMMAR",
                      $GRAMMAR_FILE,
                      $TEST{source},
                      $TEST_GRAMMAR);
    }
  }

	# Pack the grammar.
	if ($DO_PACK_GRAMMARS && ! is_packed($TEST_GRAMMAR)) {
    my $packed_dir = "$DATA_DIRS{test}/grammar.packed";

    $cachepipe->cmd("pack-test",
                    "$SCRIPTDIR/support/grammar-packer.pl -T $TMPDIR -m $PACKER_MEM $TEST_GRAMMAR $packed_dir",
                    $TEST_GRAMMAR,
                    "$packed_dir/vocabulary",
                    "$packed_dir/encoding",
                    "$packed_dir/slice_00000.source");

    # $TEST_GRAMMAR_FILE, which previously held an optional command-line argument of a pre-filtered
    # tuning grammar, is now used to record the text-based grammar, which is needed later for
    # different things.
    $TEST_GRAMMAR_FILE = $TEST_GRAMMAR;

    # The actual grammar used for decoding is the packed directory.
    $TEST_GRAMMAR = $packed_dir;
  } else {
    $TEST_GRAMMAR = $TEST_GRAMMAR_FILE;
  }
  	
  # Create the glue file.
  if (! defined $GLUE_GRAMMAR_FILE) {
    $cachepipe->cmd("glue-test",
    "java -Xmx1g -cp $JOSHUA/lib/*:$THRAX/bin/thrax.jar edu.jhu.thrax.util.CreateGlueGrammar $TEST_GRAMMAR > $DATA_DIRS{test}/grammar.glue",
    $TEST_GRAMMAR,
    "$DATA_DIRS{test}/grammar.glue");
    $GLUE_GRAMMAR_FILE = "$DATA_DIRS{test}/grammar.glue";
    
  } else {
    # just create a symlink to it
    my $filename = $DATA_DIRS{test} . "/" . basename($GLUE_GRAMMAR_FILE);
    
    if ($GLUE_GRAMMAR_FILE =~ /^\//) {
      system("ln -sf $GLUE_GRAMMAR_FILE $filename");
    } else {
      system("ln -sf $STARTDIR/$GLUE_GRAMMAR_FILE $filename");
    }
  }

  my $testrun = (defined $NAME) ? "test/$NAME/$run" : "test/$run";
  system("mkdir -p $testrun") unless -d $testrun;
  $testrun = get_absolute_path($testrun, $RUNDIR);

  # If we're decoding a lattice, also output the source side path we chose
  my $joshua_args = $JOSHUA_ARGS;
  if ($DOING_LATTICES) {
    $joshua_args .= " -maxlen 0 -output-format \"%i ||| %s ||| %e ||| %f ||| %c\"";
  }

  foreach my $key (qw(decoder_command)) {
		my $file = $TUNEFILES{$key};
		open FROM, $file or die "can't find file '$file'";
		open TO, ">$testrun/$key" or die "can't write to '$testrun/$key'";
		while (<FROM>) {
 			s/<INPUT>/$TEST{source}/g;
			s/<NUMJOBS>/$NUM_JOBS/g;
			s/<NUMTHREADS>/$NUM_THREADS/g;
			s/<QSUB_ARGS>/$QSUB_ARGS/g;
			s/<OUTPUT>/$testrun\/test.output.nbest/g;
			s/<JOSHUA>/$JOSHUA/g;
			s/<JOSHUA_ARGS>/$joshua_args/g;
			s/<NUMREFS>/$numrefs/g;
			s/<SOURCE>/$SOURCE/g;
			s/<TARGET>/$TARGET/g;
			s/<RUNDIR>/$TARGET/g;
			s/<LMFILE>/$LMFILES[0]/g;
			s/<MEM>/$JOSHUA_MEM/g;
			s/<GRAMMAR_TYPE>/$GRAMMAR_TYPE/g;
			s/<GRAMMAR_FILE>/$TEST_GRAMMAR/g;
			s/<GLUE_GRAMMAR>/$GLUE_GRAMMAR_FILE/g;
			s/<OOV>/$OOV/g;
			s/<CONFIG>/$testrun\/joshua.config/g;
			s/<LOG>/$testrun\/joshua.log/g;

			print TO;
		}
		close(FROM);
		close(TO);
  }
  chmod(0755,"$testrun/decoder_command");

  # Copy the config file over.
  $cachepipe->cmd("test-joshua-config-from-tune-$run",
                  "cat $tunedir/joshua.config.final | $COPY_CONFIG -mark-oovs true -tm 'thrax pt $MAXSPAN $TEST_GRAMMAR' > $testrun/joshua.config",
									"$tunedir/joshua.config.final",
									"$testrun/joshua.config");

  $cachepipe->cmd("test-decode-$run",
									"$testrun/decoder_command",
                  $TEST{source},
									"$DATA_DIRS{test}/grammar.glue",
									$TEST_GRAMMAR_FILE,
									"$testrun/test.output.nbest");

  $cachepipe->cmd("remove-oov-$run",
									"cat $testrun/test.output.nbest | perl -pe 's/_OOV//g' > $testrun/test.output.nbest.noOOV",
									"$testrun/test.output.nbest",
									"$testrun/test.output.nbest.noOOV");

  my $output = "$testrun/test.output.1best";
  $numrefs = get_numrefs($TEST{target});

  # Always compute the BLEU score on the regular 1-best output, since it's easy to do
  $cachepipe->cmd("test-extract-onebest-$run",
                  "java -Xmx500m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.util.ExtractTopCand $testrun/test.output.nbest.noOOV $output",
                  "$testrun/test.output.nbest.noOOV", 
                  $output);

  $cachepipe->cmd("test-bleu-$run",
									"java -cp $JOSHUA/class -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand $output -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > $testrun/test.output.1best.bleu",
									$output,
									"$output.bleu");

  # We can also rescore the output lattice with MBR
  if ($DO_MBR) {
		my $numlines = `cat $TEST{source} | wc -l`;
		$numlines--;
    $output .= ".mbr";

		$cachepipe->cmd("test-onebest-parmbr-$run", 
										"cat $testrun/test.output.nbest.noOOV | java -Xmx1700m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.decoder.NbestMinRiskReranker false 1 $NUM_THREADS > $output",
										"$testrun/test.output.nbest.noOOV", 
										$output);

    $cachepipe->cmd("test-bleu-mbr-$run",
                    "java -cp $JOSHUA/class -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand $output -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > $testrun/test.output.1best.mbr.bleu",
                    $output,
                    "$output.bleu");
  }

  # Update the BLEU summary.
  my $dir = (defined $NAME) ? "test/$NAME" : "test";
  compute_bleu_summary("$dir/*/*.1best.bleu", "$dir/final-bleu");
  compute_bleu_summary("$dir/*/*.1best.mbr.bleu", "$dir/final-bleu-mbr");
  compute_time_summary("$dir/*/joshua.log", "$dir/final-times");

  # Now do the analysis
  if ($DOING_LATTICES) {
    # extract the source
    my $source = "$testrun/test.lattice-path.txt";
    $cachepipe->cmd("test-lattice-extract-source-$run",
                    "$JOSHUA/bin/extract-1best $testrun/test.output.nbest.noOOV 2 | perl -pe 's/<s> //' > $source",
                    $output, $source);

    analyze_testrun($output,$source,$TEST{target});
  } else {
    analyze_testrun($output,$TEST{source},$TEST{target});
  }
}

exit;

# This target allows the pipeline to be used just for decoding new
# data sets

TEST:
    ;

system("mkdir -p $DATA_DIRS{test}") unless -d $DATA_DIRS{test};

if (! defined $NAME) {
  print "* FATAL: for direct tests, you must specify a unique run name\n";
  exit 1;
}

# if (-e "$DATA_DIRS{test}/$NAME") {
#   print "* FATAL: you specified a run name, but it already exists\n";
#   exit 1;
# }

if (! $PREPPED{TEST} and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("test",[$TEST],$MAXLEN_TEST);
  $TEST{source} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$SOURCE";
  $TEST{target} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TEST} = 1;
}

my $testrun = "test/$NAME";
system("mkdir -p $testrun") unless -d $testrun;

# filter the test grammar
my $TEST_GRAMMAR;
if ($TEST_GRAMMAR_FILE) {
  # if a specific test grammar was specified, use that (no filtering)
  $TEST_GRAMMAR = $TEST_GRAMMAR_FILE;
} else {
  # otherwise, use the main grammar, and filter it if requested
  $TEST_GRAMMAR = $GRAMMAR_FILE;
  
  if ($DO_FILTER_TM and ! $DOING_LATTICES) {
		$TEST_GRAMMAR = "$DATA_DIRS{test}/grammar.filtered.gz";

		$cachepipe->cmd("filter-test-$NAME",
										"$CAT $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter $FILTERING -v $TEST{source} | $SCRIPTDIR/training/filter-rules.pl -bus$SCOPE | gzip -9n > $TEST_GRAMMAR",
										$GRAMMAR_FILE,
										$TEST{source},
										$TEST_GRAMMAR);
  }
}

# build the glue grammar if needed
if (! defined $GLUE_GRAMMAR_FILE) {
  $cachepipe->cmd("glue-test-$NAME",
									"java -Xmx2g -cp $JOSHUA/lib/*:$THRAX/bin/thrax.jar edu.jhu.thrax.util.CreateGlueGrammar $TEST_GRAMMAR > $DATA_DIRS{test}/grammar.glue",
									$TEST_GRAMMAR,
									"$DATA_DIRS{test}/grammar.glue");
  $GLUE_GRAMMAR_FILE = "$DATA_DIRS{test}/grammar.glue";
}

if ($TUNEFILES{'joshua.config'} eq $JOSHUA_CONFIG_ORIG) {
  print "* FATAL: for direct tests, I need a (tuned) Joshua config file\n";
  exit 1;
}

if ($DO_PACK_GRAMMARS && ! is_packed($TEST_GRAMMAR)) {
  my $packed_dir = "$DATA_DIRS{test}/grammar.packed";

  $cachepipe->cmd("pack-test",
                  "$SCRIPTDIR/support/grammar-packer.pl -T $TMPDIR -m $PACKER_MEM $TEST_GRAMMAR $packed_dir",
                  $TEST_GRAMMAR,
                  "$packed_dir/vocabulary",
                  "$packed_dir/encoding",
                  "$packed_dir/slice_00000.source");

  # $TEST_GRAMMAR_FILE, which previously held an optional command-line argument of a pre-filtered
  # tuning grammar, is now used to record the text-based grammar, which is needed later for
  # different things.
  $TEST_GRAMMAR_FILE = $TEST_GRAMMAR;

  # The actual grammar used for decoding is the packed directory.
  $TEST_GRAMMAR = $packed_dir;
} else {
  $TEST_GRAMMAR = $TEST_GRAMMAR_FILE;
}

# this needs to be in a function since it is done all over the place
open FROM, $TUNEFILES{decoder_command} or die "can't find file '$TUNEFILES{decoder_command}'";
open TO, ">$testrun/decoder_command";
print TO "cat $TEST{source} | \$JOSHUA/bin/joshua-decoder -m $JOSHUA_MEM -threads $NUM_THREADS -c $testrun/joshua.config > $testrun/test.output.nbest 2> $testrun/joshua.log\n";
close(TO);
chmod(0755,"$testrun/decoder_command");

# copy over the config file
$cachepipe->cmd("test-$NAME-copy-config",
                "cat $TUNEFILES{'joshua.config'} | $COPY_CONFIG -mark-oovs true -tm/pt 'thrax pt $MAXSPAN $TEST_GRAMMAR' -default-non-terminal $OOV > $testrun/joshua.config",
                $TUNEFILES{'joshua.config'},
                "$testrun/joshua.config");

# decode
$cachepipe->cmd("test-$NAME-decode-run",
								"$testrun/decoder_command",
                $TEST{source},
								$TEST_GRAMMAR,
								$GLUE_GRAMMAR_FILE,
								"$testrun/test.output.nbest");

$cachepipe->cmd("test-$NAME-remove-oov",
								"cat $testrun/test.output.nbest | perl -pe 's/_OOV//g' > $testrun/test.output.nbest.noOOV",
								"$testrun/test.output.nbest",
								"$testrun/test.output.nbest.noOOV");

if ($DO_MBR) {
  $cachepipe->cmd("test-$NAME-onebest-parmbr", 
									"cat $testrun/test.output.nbest.noOOV | java -Xmx1700m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.decoder.NbestMinRiskReranker false 1 > $testrun/test.output.1best",
									"$testrun/test.output.nbest.noOOV", 
									"$testrun/test.output.1best");
} else {
  $cachepipe->cmd("test-$NAME-extract-onebest",
									"java -Xmx500m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.util.ExtractTopCand $testrun/test.output.nbest.noOOV $testrun/test.output.1best",
									"$testrun/test.output.nbest.noOOV", 
									"$testrun/test.output.1best");
}

$numrefs = get_numrefs($TEST{target});
$cachepipe->cmd("$NAME-test-bleu",
								"java -cp $JOSHUA/class -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand $testrun/test.output.1best -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > $testrun/test.output.1best.bleu",
								"$testrun/test.output.1best", 
								"$testrun/test.output.1best.bleu");

system("cat $testrun/test.output.1best.bleu");


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
  $maxlen = 0 unless defined $maxlen;

  system("mkdir -p $DATA_DIR") unless -d $DATA_DIR;
  system("mkdir -p $DATA_DIRS{$label}") unless -d $DATA_DIRS{$label};

  # records the pieces that are produced
  my %prefixes;

  # copy the data from its original location to our location
	my $numlines = -1;
  foreach my $ext ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
    # append each extension to the corpora prefixes
    my @files = map { "$_.$ext" } @$corpora;

		# This block makes sure that the files have a nonzero file size
		map {
			if (-z $_) {
				print STDERR "* FATAL: $label file '$_' is empty";
				exit 1;
			}
		} @files;

    # a list of all the files (in case of multiple corpora prefixes)
    my $files = join(" ",@files);
    if (-e $files[0]) {
      $cachepipe->cmd("$label-copy-$ext",
                      "cat $files | gzip -9n > $DATA_DIRS{$label}/$label.$ext.gz",
                      @files, "$DATA_DIRS{$label}/$label.$ext.gz");

			chomp(my $lines = `$CAT $DATA_DIRS{$label}/$label.$ext.gz | wc -l`);
			$numlines = $lines if ($numlines == -1);
			if ($lines != $numlines) {
				print STDERR "* FATAL: $DATA_DIRS{$label}/$label.$ext.gz has a different number of lines ($lines) than a 'parallel' file that preceded it ($numlines)\n";
				exit(1);
			}
		}
  }

  my $prefix = "$label";

  # tokenize the data
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
		if (-e "$DATA_DIRS{$label}/$prefix.$lang.gz") {
			if (is_lattice("$DATA_DIRS{$label}/$prefix.$lang.gz")) { 
				system("cp $DATA_DIRS{$label}/$prefix.$lang.gz $DATA_DIRS{$label}/$prefix.tok.$lang.gz");
			} else {
        my $TOKENIZER = ($lang eq $SOURCE) ? $TOKENIZER_SOURCE : $TOKENIZER_TARGET;
	my $ext = $lang; $ext =~ s/\.\d//;
				$cachepipe->cmd("$label-tokenize-$lang",
												"$CAT $DATA_DIRS{$label}/$prefix.$lang.gz | $NORMALIZER $ext | $TOKENIZER -l $ext 2> /dev/null | gzip -9n > $DATA_DIRS{$label}/$prefix.tok.$lang.gz",
												"$DATA_DIRS{$label}/$prefix.$lang.gz", "$DATA_DIRS{$label}/$prefix.tok.$lang.gz");
			}

		}
  }
  # extend the prefix
  $prefix .= ".tok";
  $prefixes{tokenized} = $prefix;

  if ($maxlen > 0) {
    my (@infiles, @outfiles);
    foreach my $ext ($TARGET, $SOURCE, "$TARGET.0", "$TARGET.1", "$TARGET.2", "$TARGET.3") {
      my $infile = "$DATA_DIRS{$label}/$prefix.$ext.gz";
      my $outfile = "$DATA_DIRS{$label}/$prefix.$maxlen.$ext.gz";
      if (-e $infile) {
        push(@infiles, $infile);
        push(@outfiles, $outfile);
      }
    }

    my $infilelist = join(" ", map { "<(gzip -cd $_)" } @infiles);
    my $outfilelist = join(" ", @outfiles);

		# trim training data
		$cachepipe->cmd("$label-trim",
										"paste $infilelist | $SCRIPTDIR/training/trim_parallel_corpus.pl $maxlen | $SCRIPTDIR/training/split2files.pl $outfilelist",
                    @infiles,
                    @outfiles);
		$prefix .= ".$maxlen";
  }
  # record this whether we shortened or not
  $prefixes{shortened} = $prefix;

  # lowercase
  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
		if (-e "$DATA_DIRS{$label}/$prefix.$lang.gz") {
			if (is_lattice("$DATA_DIRS{$label}/$prefix.$lang.gz")) { 
				system("gzip -cd $DATA_DIRS{$label}/$prefix.$lang.gz > $DATA_DIRS{$label}/$prefix.lc.$lang");
			} else { 
				$cachepipe->cmd("$label-lowercase-$lang",
												"gzip -cd $DATA_DIRS{$label}/$prefix.$lang.gz | $SCRIPTDIR/lowercase.perl > $DATA_DIRS{$label}/$prefix.lc.$lang",
												"$DATA_DIRS{$label}/$prefix.$lang.gz",
												"$DATA_DIRS{$label}/$prefix.lc.$lang");
			}
		}
  }
  $prefix .= ".lc";
  $prefixes{lowercased} = $prefix;

  foreach my $lang ($TARGET,$SOURCE,"$TARGET.0","$TARGET.1","$TARGET.2","$TARGET.3") {
		if (-e "$DATA_DIRS{$label}/$prefixes{lowercased}.$lang") {
      system("ln -sf $prefixes{lowercased}.$lang $DATA_DIRS{$label}/corpus.$lang");
    }
  }

  return \%prefixes;
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

  if (-e "$prefix.0") {
		my $index = 0;
		while (-e "$prefix.$index") {
			$index++;
		}
		return $index;
  } else {
		return 1;
  }
}

sub start_hadoop_cluster {
  rollout_hadoop_cluster();

  # start the cluster
  # system("./hadoop/bin/start-all.sh");
  # sleep(120);
}

sub rollout_hadoop_cluster {
  # if it's not already unpacked, unpack it
  if (! -d "hadoop") {

		system("tar xzf $JOSHUA/lib/hadoop-0.20.2.tar.gz");
		system("ln -sf hadoop-0.20.2 hadoop");
    if (defined $HADOOP_CONF) {
      print STDERR "Copying HADOOP_CONF($HADOOP_CONF) to hadoop/conf/core-site.xml\n";
      system("cp $HADOOP_CONF hadoop/conf/core-site.xml");
    }
  }
  
  $ENV{HADOOP} = $HADOOP = "hadoop";
  $ENV{HADOOP_CONF_DIR} = "";
}

sub stop_hadoop_cluster {
  if ($HADOOP ne "hadoop") {
		system("hadoop/bin/stop-all.sh");
  }
}

sub teardown_hadoop_cluster {
  stop_hadoop_cluster();
  system("rm -rf hadoop-0.20.2 hadoop");
}

sub is_lattice {
  my $file = shift;
  open READ, "$CAT $file|" or die "can't read from potential lattice '$file'";
  my $line = <READ>;
  close(READ);
  if ($line =~ /^\(\(\(/) {
		$DOING_LATTICES = 1;
		$FILTERING = "-l";
		return 1;
  } else {
		return 0;
  }
}

# This function retrieves the names of all the features in the grammar. Dense features
# are named with consecutive integers starting at 0, while sparse features can have any name.
# To get the feature names from an unpacked grammar, we have to read through the whole grammar,
# since sparse features can be anywhere. For packed grammars, this can be read directly from
# the encoding.
sub get_features {
  my ($grammar) = @_;

  if (-d $grammar) {
    chomp(my @features = `java -cp $JOSHUA/class joshua.util.encoding.EncoderConfiguration $grammar | grep ^feature: | awk '{print \$NF}'`);
    return @features;

  } elsif (-e $grammar) {
    my %features;
    open GRAMMAR, "$CAT $grammar|" or die "FATAL: can't read $grammar";
    while (my $line = <GRAMMAR>) {
      chomp($line);
      my @tokens = split(/ \|\|\| /, $line);
      my $feature_str = $tokens[3];
      my @features = split(' ', $feature_str);
      my $feature_no = 0;
      foreach my $feature (@features) {
        if ($feature =~ /=/) {
          my ($name) = split(/=/, $feature);
          $features{$name} = 1;
        } else {
          $features{$feature_no++} = 1;
        }
      } 
    }
    close(GRAMMAR);
    return keys(%features);
  }
}

# File names reflecting relative paths need to be absolute-ized for --rundir to work.
# Does not work with paths that do not exist!
sub get_absolute_path {
  my ($file,$basedir) = @_;
  $basedir = $STARTDIR unless defined $basedir;

  if (defined $file) {
    $file = "$basedir/$file" unless $file =~ /^\//;

    # prepend startdir (which is absolute) unless the path is absolute.
    my $abs_path = abs_path($file);
    if (defined $abs_path) {
      $file = $abs_path;
    }
  }

  return $file;
}

sub analyze_testrun {
  my ($output,$source,$reference) = @_;
  my $dir = dirname($output);

  mkdir("$dir/analysis") unless -d "$dir/analysis";

  my @references;
  if (-e "$reference.0") {
    my $num = 0;
    while (-e "$reference.$num") {
      push(@references, "$reference.$num");
      $num++;
    }
  } else {
    push(@references, $reference);
  }

  my $references = join(" -r ", @references);

  my $runname = "analyze-$dir";
  $runname =~ s/\//-/g;
  $cachepipe->cmd($runname,
                  "$SCRIPTDIR/analysis/sentence-by-sentence.pl -s $source -r $references $output > $dir/analysis/sentence-by-sentence.html",
                  "$dir/test.output.1best",
                  "$dir/analysis/sentence-by-sentence.html");
}

sub compute_bleu_summary {
  my ($filepattern, $outputfile) = @_;

  # Now average the runs, report BLEU
  my @bleus;
  my $numrecs = 0;
  open CMD, "grep ' BLEU = ' $filepattern |";
  while (<CMD>) {
    my @F = split;
    push(@bleus, 1.0 * $F[-1]);
  }
  close(CMD);

  if (scalar @bleus) {
    my $final_bleu = sum(@bleus) / (scalar @bleus);

    open BLEU, ">$outputfile" or die "Can't write to $outputfile";
    printf(BLEU "%s / %d = %.4f\n", join(" + ", @bleus), scalar @bleus, $final_bleu);
    close(BLEU);
  }
}

sub compute_time_summary {
  my ($filepattern, $outputfile) = @_;

  # Now average the runs, report BLEU
  my @times;
  foreach my $file (glob($filepattern)) {
    open FILE, $file;
    my $time = 0.0;
    my $numrecs = 0;
    while (<FILE>) {
      next unless /translation of .* took/;
      my @F = split;
      $time += $F[5];
      $numrecs++;
    }
    close(FILE);

    push(@times, $time);
  }

  if (scalar @times) {
    open TIMES, ">$outputfile" or die "Can't write to $outputfile";
    printf(TIMES "%s / %d = %s\n", join(" + ", @times), scalar(@times), 1.0 * sum(@times) / scalar(@times));
    close(TIMES);
  }
}

sub is_packed {
  my ($grammar) = @_;

  if (-d $grammar && -e "$grammar/encoding") {
    return 1;
  }

  return 0;
}
