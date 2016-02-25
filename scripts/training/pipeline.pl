#!/usr/bin/env perl

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
use File::Temp qw[:mktemp tempdir];
use CachePipe;

# There are some Perl 5.10 Unicode bugs that cause problems, mostly in sub-scripts
use v5.12;
# use Thread::Pool;

# Hadoop uses a stupid hacker trick to change directories, but (per Lane Schwartz) if CDPATH
# contains ".", it triggers the printing of the directory, which kills the stupid hacker trick.
# Thus we undefine CDPATH to ensure this doesn't happen.
delete $ENV{CDPATH};

my $HADOOP = $ENV{HADOOP};
my $MOSES = $ENV{MOSES};
my $METEOR = $ENV{METEOR};
my $THRAX = "$JOSHUA/thrax";
delete $ENV{GREP_OPTIONS};

die not_defined("JAVA_HOME") unless exists $ENV{JAVA_HOME};

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$SOURCE,$TARGET,@LMFILES,$GRAMMAR_FILE,$GLUE_GRAMMAR_FILE,$_TUNE_GRAMMAR_FILE,$_TEST_GRAMMAR_FILE,$THRAX_CONF_FILE, $_JOSHUA_CONFIG, $_JOSHUA_ARGS);
my $FIRST_STEP = "SUBSAMPLE";
my $LAST_STEP  = "LAST";
my $LMFILTER = "$ENV{HOME}/code/filter/filter";

# The maximum length of training sentences (--maxlen). The threshold is applied to both sides.
my $MAXLEN = 50;

# The maximum span rules in the main grammar can be applied to
my $MAXSPAN = 20;

# The maximum length of tuning and testing sentences (--maxlen-tune and --maxlen-test).
my $MAXLEN_TUNE = 0;
my $MAXLEN_TEST = 0;

# when doing phrase-based decoding, the maximum length of a phrase (source side)
my $MAX_PHRASE_LEN = 5;

my $DO_FILTER_TM = 1;
my $DO_SUBSAMPLE = 0;
my $DO_PACK_GRAMMARS = 1;
my $SCRIPTDIR = "$JOSHUA/scripts";
my $TOKENIZER_SOURCE = "$SCRIPTDIR/preparation/tokenize.pl";
my $TOKENIZER_TARGET = "$SCRIPTDIR/preparation/detokenize.pl";
my $NORMALIZER = "$SCRIPTDIR/preparation/normalize.pl";
my $LOWERCASER = "$SCRIPTDIR/preparation/lowercase.pl";
my $GIZA_TRAINER = "$SCRIPTDIR/training/run-giza.pl";
my $TUNECONFDIR = "$SCRIPTDIR/training/templates/tune";
my $SRILM = ($ENV{SRILM}||"")."/bin/i686-m64/ngram-count";
my $COPY_CONFIG = "$SCRIPTDIR/copy-config.pl";
my $BUNDLER = "$JOSHUA/scripts/support/run_bundler.py";
my $STARTDIR;
my $RUNDIR = $STARTDIR = getcwd();
my $GRAMMAR_TYPE = undef; # hiero, itg, samt, ghkm, phrase, or moses
my $SEARCH_ALGORITHM = "cky"; # or "stack" (for phrase-based)

# Which GHKM extractor to use ("galley" or "moses")
my $GHKM_EXTRACTOR = "moses";
my $EXTRACT_OPTIONS = "";

my $WITTEN_BELL = 0;

# Run description.
my $README = undef;

# gzip-aware cat
my $CAT = "$SCRIPTDIR/training/scat";

# custom version of paste that dies on unequal file lengths
my $PASTE = "$SCRIPTDIR/training/paste";

# where processed data files are stored
my $DATA_DIR = "data";

# Whether to do MBR decoding on the n-best list (for test data).
my $DO_MBR = 0;

# Which aligner to use. The options are "giza" or "berkeley".
my $ALIGNER = "giza"; # "berkeley" or "giza" or "jacana"
my $ALIGNER_CONF = "$JOSHUA/scripts/training/templates/alignment/word-align.conf";

# Filter rules to the following maximum scope (Hopkins & Langmead, 2011).
my $SCOPE = 3;

# What kind of filtering to use ("fast" or "exact").
my $FILTERING = "fast";

# This is the amount of memory made available to Joshua.  You'll need
# a lot more than this for SAMT decoding (though really it depends
# mostly on your grammar size)
my $JOSHUA_MEM = "4g";

# the amount of memory available for hadoop processes (passed to
# Hadoop via -Dmapred.child.java.opts
my $HADOOP_MEM = "4g";

# The location of a custom core-site.xml file, if desired (optional).
my $HADOOP_CONF = undef;

# memory available to the parser
my $PARSER_MEM = "2g";

# memory available for building the language model
my $BUILDLM_MEM = "2G";

# Memory available for packing the grammar.
my $PACKER_MEM = "8g";

# Memory available for MERT/PRO.
my $TUNER_MEM = "8g";

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

# Whether to build and include an LM from the target-side of the
# corpus when manually-specified LM files are passed with --lmfile.
my $DO_BUILD_CLASS_LM = 0;
my $CLASS_LM_CORPUS = undef;
my $CLASS_MAP = undef;
my $CLASS_LM_ORDER = 5;

# whether to tokenize and lowercase training, tuning, and test data
my $DO_PREPARE_CORPORA = 1;

# compute the nth optimizer run
my $OPTIMIZER_RUN = 1;

# what to use to create language models ("berkeleylm" or "srilm")
my $LM_GEN = "kenlm";
my $LM_OPTIONS = "";

my @STEPS = qw[FIRST SUBSAMPLE ALIGN PARSE THRAX MODEL GRAMMAR PHRASE TUNE MERT PRO TEST LAST];
my %STEPS = map { $STEPS[$_] => $_ + 1 } (0..$#STEPS);

# Methods to use for merging alignments (see Koehn et al., 2003).
# Options are union, {intersect, grow, srctotgt, tgttosrc}-{diag,final,final-and,diag-final,diag-final-and}
my $GIZA_MERGE = "grow-diag-final";

# Whether to merge all the --lmfile LMs into a single LM using weights based on the development corpus
my $MERGE_LMS = 0;

# Which tuner to use by default
my @TUNERS = ("mert", "pro", "mira", "adagrad", "kbmira");
my $TUNER = "mert";

# The metric to update to
my $METRIC = "BLEU 4 closest";

# The number of iterations of the tuner to run
my $TUNER_ITERATIONS = 10;

# location of already-parsed corpus
my $PARSED_CORPUS = undef;

# location of the ner tagger wrapper script for annotation
my $NER_TAGGER = undef;

# Allows the user to set a temp dir for various tasks
my $TMPDIR = "/tmp";

# Enable forest rescoring
my $LM_STATE_MINIMIZATION = 1;

my $NBEST = 300;

my $REORDERING_LIMIT = 6;
my $NUM_TRANSLATION_OPTIONS = 20;

my $retval = GetOptions(
  "readme=s"    => \$README,
  "corpus=s"        => \@CORPORA,
  "parsed-corpus=s"   => \$PARSED_CORPUS,
  "tune=s"          => \$TUNE,
  "test=s"            => \$TEST,
  "prepare!"          => \$DO_PREPARE_CORPORA,
  "aligner=s"         => \$ALIGNER,
  "alignment=s"      => \$ALIGNMENT,
  "aligner-mem=s"     => \$ALIGNER_MEM,
  "aligner-conf=s"   => \$ALIGNER_CONF,
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
  "lm-gen-options=s"          => \$LM_OPTIONS,
  "lm-order=i"        => \$LM_ORDER,
  "corpus-lm!"        => \$DO_BUILD_LM_FROM_CORPUS,
  "witten-bell!"     => \$WITTEN_BELL,
  "tune-grammar=s"    => \$_TUNE_GRAMMAR_FILE,
  "test-grammar=s"    => \$_TEST_GRAMMAR_FILE,
  "grammar=s"        => \$GRAMMAR_FILE,
  "model=s"          => \$GRAMMAR_FILE,
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
  "normalizer=s"      => \$NORMALIZER,
  "joshua-config=s"   => \$_JOSHUA_CONFIG,
  "joshua-args=s"      => \$_JOSHUA_ARGS,
  "joshua-mem=s"      => \$JOSHUA_MEM,
  "hadoop-mem=s"      => \$HADOOP_MEM,
  "parser-mem=s"      => \$PARSER_MEM,
  "buildlm-mem=s"     => \$BUILDLM_MEM,
  "packer-mem=s"      => \$PACKER_MEM,
  "pack!"             => \$DO_PACK_GRAMMARS,
  "tuner=s"           => \$TUNER,
  "tuner-mem=s"       => \$TUNER_MEM,
  "tuner-iterations=i" => \$TUNER_ITERATIONS,
  "tuner-metric=s"    => \$METRIC,
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
  "tmp=s"             => \$TMPDIR,
  "nbest=i"           => \$NBEST,
  "reordering-limit=i" => \$REORDERING_LIMIT,
  "num-translation-options=i" => \$NUM_TRANSLATION_OPTIONS,
  "ner-tagger=s"   => \$NER_TAGGER,
  "class-lm!"     => \$DO_BUILD_CLASS_LM,
  "class-lm-corpus=s"   => \$CLASS_LM_CORPUS,
  "class-map=s"     => \$CLASS_MAP,
  "optimizer-run=i" => \$OPTIMIZER_RUN,
);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

$RUNDIR = get_absolute_path($RUNDIR);

$TUNER = lc $TUNER;

my $DOING_LATTICES = 0;

my $JOSHUA_ARGS = (defined $_JOSHUA_ARGS) ? $_JOSHUA_ARGS : "";

my %DATA_DIRS = (
  train => get_absolute_path("$RUNDIR/$DATA_DIR/train"),
  tune  => get_absolute_path("$RUNDIR/$DATA_DIR/tune"),
  test  => get_absolute_path("$RUNDIR/$DATA_DIR/test"),
);

if (! -x $NORMALIZER) {
  print "* FATAL: couldn't find normalizer '$NORMALIZER'\n";
  exit 1;
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

my @GRAMMAR_TYPES = qw/hiero samt ghkm phrase moses/;
if (! defined $GRAMMAR_TYPE or ! in($GRAMMAR_TYPE,\@GRAMMAR_TYPES)) {
  print "* FATAL: You must define --type (" . join("|", @GRAMMAR_TYPES) . ")\n";
  exit 47;
}

# case-normalize this
$GRAMMAR_TYPE = lc $GRAMMAR_TYPE;

if ($GRAMMAR_TYPE eq "phrase" or $GRAMMAR_TYPE eq "moses") {
  $SEARCH_ALGORITHM = "stack";
  $MAXSPAN = 0;
}

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

# Joshua config
my $JOSHUA_CONFIG = get_absolute_path($_JOSHUA_CONFIG || "$TUNECONFDIR/joshua.config", $STARTDIR);

# make sure we have a tuned config file if we're skipping model building and tuning
if ($STEPS{$FIRST_STEP} >= $STEPS{TEST}) {
  if (! defined $JOSHUA_CONFIG) {
    print "* FATAL: You need to provide a tuned Joshua config file (--joshua-config)\n";
    print "         if you're skipping straight to testing\n";
    exit 1;
  }
}

# make sure we have either a config file or a grammar and LM if we're skipping model building
if ($STEPS{$FIRST_STEP} >= $STEPS{TUNE}) {
  if (! defined $JOSHUA_CONFIG and ((! defined $_TUNE_GRAMMAR_FILE and ! defined $GRAMMAR_FILE) or scalar(@LMFILES) == 0)) {
    print "* FATAL: You must provide either a Joshua config file (--joshua-config) or\n";
    print "         a grammar (--grammar or --tune-grammar) and at least one LM (--lmfile)\n";
    print "         if you're skipping straight to tuning\n";
    exit 1;
  }
}

# make sure SRILM is defined if we're building a language model
if ($LM_GEN eq "srilm" && (scalar @LMFILES == 0) && $STEPS{$FIRST_STEP} <= $STEPS{TUNE} && $STEPS{$LAST_STEP} >= $STEPS{TUNE}) {
  not_defined("SRILM") unless exists $ENV{SRILM} and -d $ENV{SRILM};
}

# check for file presence
if (defined $JOSHUA_CONFIG and ! -e $JOSHUA_CONFIG) {
  print "* FATAL: couldn't find joshua config file '$JOSHUA_CONFIG'\n";
  exit 1;
}
if (defined $GRAMMAR_FILE and ! -e $GRAMMAR_FILE) {
  print "* FATAL: couldn't find grammar file '$GRAMMAR_FILE'\n";
  exit 1;
}
if (defined $_TUNE_GRAMMAR_FILE and ! -e $_TUNE_GRAMMAR_FILE) {
  print "* FATAL: couldn't find tuning grammar file '$_TUNE_GRAMMAR_FILE'\n";
  exit 1;
}
if (defined $_TEST_GRAMMAR_FILE and ! -e $_TEST_GRAMMAR_FILE) {
  print "* FATAL: couldn't find test grammar file '$_TEST_GRAMMAR_FILE'\n";
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
$_TUNE_GRAMMAR_FILE = get_absolute_path($_TUNE_GRAMMAR_FILE);
$_TEST_GRAMMAR_FILE = get_absolute_path($_TEST_GRAMMAR_FILE);
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

if ($LM_TYPE ne "kenlm") {
  $LM_STATE_MINIMIZATION = 0;
}

if ($LM_GEN ne "berkeleylm" and $LM_GEN ne "srilm" and $LM_GEN ne "kenlm") {
  print "* FATAL: lm generating code (--lm-gen) must be one of 'kenlm' (default), 'berkeleylm', or 'srilm'\n";
  exit 1;
}

if ($TUNER eq "kbmira" and ! defined $MOSES) {
  print "* FATAL: using 'kbmira' for tuning requires setting the MOSES environment variable\n";
  exit 1;
}

if ($GRAMMAR_TYPE eq "moses" and ! defined $MOSES) {
  print "* FATAL: building Moses phrase-based models (--type moses) requires setting the MOSES environment variable\n";
  exit 1;
}

if (! in($TUNER, \@TUNERS)) {
  print "* FATAL: --tuner must be one of " . join(", ", @TUNERS) . $/;
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

my $OOV = ($GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "itg" or $GRAMMAR_TYPE eq "phrase" or $GRAMMAR_TYPE eq "moses") ? "X" : "OOV";

# The phrasal system should use the ITG grammar, allowing for limited distortion
if ($GRAMMAR_TYPE eq "phrasal") {
  $GLUE_GRAMMAR_FILE = get_absolute_path("$JOSHUA/scripts/training/templates/glue-grammar.itg");
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

# Record the preprocessing scripts that were used
mkdir("scripts") unless -e "scripts";
unlink "scripts/normalize.$SOURCE";
unlink "scripts/normalize.$TARGET";
symlink $NORMALIZER, "scripts/normalize.$SOURCE";
symlink $NORMALIZER, "scripts/normalize.$TARGET";
unlink "scripts/tokenize.$SOURCE";
unlink "scripts/tokenize.$TARGET";
symlink $TOKENIZER_SOURCE, "scripts/tokenize.$SOURCE";
symlink $TOKENIZER_TARGET, "scripts/tokenize.$TARGET";

## STEP 1: filter and preprocess corpora #############################

if (defined $ALIGNMENT and $STEPS{$FIRST_STEP} < $STEPS{ALIGN}) {
  print "* FATAL: it doesn't make sense to provide an alignment and then do\n";
  print "  tokenization.  Either remove --alignment or specify a first step\n";
  print "  of Thrax (--first-step THRAX)\n";
  exit 1;
}

if (@CORPORA == 0 and $STEPS{$FIRST_STEP} < $STEPS{TUNE}) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

# prepare the training data
my %PREPPED = (
  TRAIN => 0,
  TUNE => 0,
  TEST => 0);

if (@CORPORA > 0) {
  my $prefixes = prepare_data("train",\@CORPORA,$MAXLEN);

  # used for parsing
  if (exists $prefixes->{shortened}) {
    $TRAIN{mixedcase} = "$DATA_DIRS{train}/$prefixes->{shortened}.$TARGET.gz";
  }

  $TRAIN{prefix} = "$DATA_DIRS{train}/corpus";
  $TRAIN{source} = "$DATA_DIRS{train}/corpus.$SOURCE";
  $TRAIN{target} = "$DATA_DIRS{train}/corpus.$TARGET";
  $PREPPED{TRAIN} = 1;
}

# prepare the tuning and development data
if (defined $TUNE) {
  my $prefixes = prepare_data("tune",[$TUNE],$MAXLEN_TUNE);
  $TUNE{source} = "$DATA_DIRS{tune}/corpus.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/corpus.$TARGET";
  my $ner_return = ner_annotate("$TUNE{source}", "$TUNE{source}.ner", $SOURCE);
  if ($ner_return == 2) {
    $TUNE{source} = "$TUNE{source}.ner";
  }
  $PREPPED{TUNE} = 1;
}

if (defined $TEST) {
  my $prefixes = prepare_data("test",[$TEST],$MAXLEN_TEST);
  $TEST{source} = "$DATA_DIRS{test}/corpus.$SOURCE";
  $TEST{target} = "$DATA_DIRS{test}/corpus.$TARGET";
  my $ner_return = ner_annotate("$TEST{source}", "$TEST{source}.ner", $SOURCE);
  if ($ner_return == 2) {
    $TEST{source} = "$TEST{source}.ner";
  }
  $PREPPED{TEST} = 1;
}

## Use of GOTO considered very useful
if (eval { goto $FIRST_STEP }) {
  print "* Skipping to step $FIRST_STEP\n";
  goto $FIRST_STEP;
} else {
  print "* No such step $FIRST_STEP\n";
  exit 1;
}

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

  my $aligner_cmd = (
    "$SCRIPTDIR/training/paralign.pl "
    . " -aligner $ALIGNER"
    . " -conf $ALIGNER_CONF"
    . " -num_threads 2"
    . " -giza_merge $GIZA_MERGE"
    . " -aligner_mem $ALIGNER_MEM"
    . " -source $SOURCE"
    . " -target $TARGET"
    . " -giza_trainer \"$GIZA_TRAINER\""
    . " -train_dir \"$DATA_DIRS{train}\" "
    . "> alignments/run.log"
  );

  # Start a parallel job on each core
  my @children = ();
  my $next_chunk = 0;
  foreach my $core (1..$NUM_THREADS) {
    if ($next_chunk < $lastchunk + 1) {
      my $child = fork();
      if (! $child) { # I am child
        exec("echo $next_chunk | $aligner_cmd");
        exit 0;
      }
      push @children, $child;
      $next_chunk++;
      next;
    }
  }

  # Start another concurrent job as each oldest job finishes
  while (@children) {
    my $old_child = shift @children;
    waitpid( $old_child, 0 );

    if ($next_chunk < $lastchunk + 1) {
      my $new_child = fork();
      if (! $new_child) { # I am child
        exec("echo $next_chunk | $aligner_cmd");
        exit 0;
      }
      $next_chunk++;
      push @children, $new_child;
    }
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

if ($FIRST_STEP eq "PARSE" and ($GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "phrasal" or $GRAMMAR_TYPE eq "phrase" or $GRAMMAR_TYPE eq "moses")) {
  print STDERR "* FATAL: parsing only applies to GHKM and SAMT grammars; you need to add '--type samt|ghkm'\n";
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

MODEL:
    ;
GRAMMAR:
    ;
THRAX:
    ;
PHRASE:
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


# Since this is an expensive step, we short-circuit it if the grammar file is present.  I'm not
# sure that this is the right behavior.
if (-e "grammar.gz" && ! -z "grammar.gz") {
  chomp(my $is_empty = `gzip -cd grammar.gz | head | wc -l`);
  $GRAMMAR_FILE = "grammar.gz" unless ($is_empty == 0);
}

# If the grammar file wasn't specified, or found, we need to build it!
if (! defined $GRAMMAR_FILE) {

  my $target_file = ($GRAMMAR_TYPE eq "ghkm" or $GRAMMAR_TYPE eq "samt") ? $TRAIN{parsed} : $TRAIN{target};

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
                      "gzip -cd model/rule-table.gz | /home/hltcoe/mpost/code/joshua/scripts/support/moses2joshua_grammar.pl -m rule-fragment-map.txt | gzip -9n > grammar.gz",
                      "model/rule-table.gz",
                      "grammar.gz");

    } else {
      print STDERR "* FATAL: no such GHKM extractor '$GHKM_EXTRACTOR'\n";
      exit(1);
    }

    $GRAMMAR_FILE = "grammar.gz";

  } elsif ($GRAMMAR_TYPE eq "moses") {

    mkdir("model") unless -d "model";

    if ($ALIGNMENT ne "alignments/training.align") {
      system("mkdir alignments") unless -d "alignments";
      system("ln -sf $ALIGNMENT alignments/training.align");
      $ALIGNMENT = "alignments/training.align";
    }

    # Compute lexical probabilities
    $cachepipe->cmd("build-lex-trans",
                    "$MOSES/scripts/training/train-model.perl -mgiza -mgiza-cpus $NUM_THREADS -dont-zip -first-step 4 -last-step 4 -external-bin-dir $MOSES/bin -f $SOURCE -e $TARGET -max-phrase-length $MAX_PHRASE_LEN -score-options '--GoodTuring' -parallel -lexical-file model/lex -alignment-file alignments/training -alignment align -corpus $TRAIN{prefix}",
                    $TRAIN{source},
                    $TRAIN{target},
                    $ALIGNMENT,
                    "model/lex.e2f",
                    "model/lex.f2e"
        );

    # Extract the phrases
    $cachepipe->cmd("extract-phrases",
                    "$MOSES/scripts/training/train-model.perl -mgiza -mgiza-cpus $NUM_THREADS -dont-zip -first-step 5 -last-step 5 -external-bin-dir $MOSES/bin -f $SOURCE -e $TARGET -max-phrase-length $MAX_PHRASE_LEN -score-options '--GoodTuring' -parallel -alignment-file alignments/training -alignment align -extract-file model/extract -corpus $TRAIN{prefix}",
                    $TRAIN{source},
                    $TRAIN{target},
                    $ALIGNMENT,
                    "model/extract.sorted.gz",
                    "model/extract.inv.sorted.gz"
        );

    # Build the phrase table
    $cachepipe->cmd("build-ttable",
                    "$MOSES/scripts/training/train-model.perl -mgiza -mgiza-cpus $NUM_THREADS -dont-zip -first-step 6 -last-step 6 -external-bin-dir $MOSES/bin -f $SOURCE -e $TARGET -alignment grow-diag-final-and -max-phrase-length $MAX_PHRASE_LEN -score-options '--GoodTuring' -parallel -extract-file model/extract -lexical-file model/lex -phrase-translation-table model/phrase-table",
                    "model/lex.e2f",
                    "model/extract.sorted.gz",
                    "model/phrase-table.gz",
        );

    $GRAMMAR_FILE = "model/phrase-table.gz";

  } elsif ($GRAMMAR_TYPE eq "samt" or $GRAMMAR_TYPE eq "hiero" or $GRAMMAR_TYPE eq "phrase") {

    # create the input file
    $cachepipe->cmd("thrax-input-file",
                    "$PASTE $TRAIN{source} $target_file $ALIGNMENT | perl -pe 's/\\t/ ||| /g' | grep -v '()' | grep -v '||| \\+\$' > $DATA_DIRS{train}/thrax-input-file",
                    $TRAIN{source}, $target_file, $ALIGNMENT,
                    "$DATA_DIRS{train}/thrax-input-file");

    # Rollout the hadoop cluster if needed.  This causes $HADOOP to be defined (pointing to the
    # unrolled directory).
    start_hadoop_cluster() unless defined $HADOOP;

    # put the hadoop files in place
    my $THRAXDIR;
    my $thrax_input;
    if (! defined $HADOOP or $HADOOP eq "") {
      $THRAXDIR = "thrax";

      $thrax_input = "$DATA_DIRS{train}/thrax-input-file"

    } else {
      $THRAXDIR = "pipeline-$SOURCE-$TARGET-$GRAMMAR_TYPE-$RUNDIR";
      $THRAXDIR =~ s#/#_#g;

      $cachepipe->cmd("thrax-prep",
                      "$HADOOP/bin/hadoop fs -rm -r $THRAXDIR; $HADOOP/bin/hadoop fs -mkdir $THRAXDIR; $HADOOP/bin/hadoop fs -put $DATA_DIRS{train}/thrax-input-file $THRAXDIR/input-file",
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
                    "$HADOOP/bin/hadoop jar $THRAX/bin/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' -D hadoop.tmp.dir=$TMPDIR $thrax_file $THRAXDIR > thrax.log 2>&1; rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $THRAXDIR/final/ grammar.gz", #; $HADOOP/bin/hadoop fs -rm -r $THRAXDIR",
                    "$DATA_DIRS{train}/thrax-input-file",
                    $thrax_file,
                    "grammar.gz");
#perl -pi -e 's/\.?0+\b//g' grammar; 

    stop_hadoop_cluster() if $HADOOP eq "hadoop";

    # cache the thrax-prep step, which depends on grammar.gz
#    if ($HADOOP ne "hadoop") {
#      $cachepipe->cmd("thrax-prep", "--cache-only");
#    }

    # clean up
    # TODO: clean up real hadoop clusters too
    # if ($HADOOP eq "hadoop") {
    #   system("rm -rf $THRAXDIR hadoop hadoop-2.5.2");
    # }

    $GRAMMAR_FILE = "grammar.gz";
  } else {

    print STDERR "* FATAL: There was no way to build a grammar, and none was passed in\n";
    print STDERR "*        Please try one of the following:\n";
    print STDERR "*        - Specify a grammar with --grammar /path/to/grammar\n";
    print STDERR "*        - Delete any existing grammar named 'grammar.gz'\n";

    exit 1;
  }
}

maybe_quit("THRAX");
maybe_quit("GRAMMAR");

## TUNING ##############################################################
TUNE:
    ;

# prep the tuning data, unless already prepped
if (! $PREPPED{TUNE}) {
  my $prefixes = prepare_data("tune",[$TUNE],$MAXLEN_TUNE);
  $TUNE{source} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TUNE} = 1;
}


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

sub compile_lm($) {
  my $lmfile = shift;
  if ($LM_TYPE eq "kenlm") {
    my $kenlm_file = basename($lmfile, ".gz") . ".kenlm";
    $cachepipe->cmd("compile-kenlm",
                    "$JOSHUA/bin/build_binary $lmfile $kenlm_file",
                    $lmfile, $kenlm_file);
    return $kenlm_file;

  } elsif ($LM_TYPE eq "berkeleylm") {
    my $berkeleylm_file = basename($lmfile, ".gz") . ".berkeleylm";
    $cachepipe->cmd("compile-berkeleylm",
                    "$JOSHUA/scripts/lm/compile_berkeley.py -m $BUILDLM_MEM $lmfile $berkeleylm_file",
                    $lmfile, $berkeleylm_file);
    return $berkeleylm_file;

  } else {
    print "* FATAL: trying to compile an LM to neither kenlm nor berkeleylm.";
    exit 2;
  }
}

# Build the language model if needed
if (defined $TRAIN{target} and $DO_BUILD_LM_FROM_CORPUS) {

  # make sure the training data is prepped
  if (! $PREPPED{TRAIN}) {
		my $prefixes = prepare_data("train", \@CORPORA, $MAXLEN);

		$TRAIN{prefix} = "$DATA_DIRS{train}/corpus";
		foreach my $lang ($SOURCE,$TARGET) {
			system("ln -sf $prefixes->{lowercased}.$lang $DATA_DIRS{train}/corpus.$lang");
		}
		$TRAIN{source} = "$DATA_DIRS{train}/corpus.$SOURCE";
		$TRAIN{target} = "$DATA_DIRS{train}/corpus.$TARGET";
		$PREPPED{TRAIN} = 1;
  }

  my $lmfile = "lm.gz";

  # sort and uniq the training data
  $cachepipe->cmd("lm-sort-uniq",
                  "$CAT $TRAIN{target} | sort -u -T $TMPDIR -S $BUILDLM_MEM | gzip -9n > $TRAIN{target}.uniq",
                  $TRAIN{target},
                  "$TRAIN{target}.uniq");

  # If an NER Tagger is specified, use that to annotate the corpus before 
  # sending it off to the LM
  my $ner_return = ner_annotate("$TRAIN{target}.uniq", "$TRAIN{target}.uniq.ner", $TARGET);
  if ($ner_return == 2) {
    $TRAIN{ner_lm} = 1;
  }

  my $lm_input = "$TRAIN{target}.uniq";
  # Choose LM input based on whether an annotated corpus was created
  if (defined $TRAIN{ner_lm}) {
    $lm_input = replace_tokens_with_types("$TRAIN{target}.uniq.ner");
  }

  if ($LM_GEN eq "srilm") {
		my $smoothing = ($WITTEN_BELL) ? "-wbdiscount" : "-kndiscount";
		$cachepipe->cmd("srilm",
										"$SRILM -order $LM_ORDER -interpolate $smoothing -unk -gt3min 1 -gt4min 1 -gt5min 1 -text $TRAIN{target}.uniq $LM_OPTIONS -lm lm.gz",
                    "$lm_input",
										$lmfile);
  } elsif ($LM_GEN eq "berkeleylm") {
		$cachepipe->cmd("berkeleylm",
				"java -ea -mx$BUILDLM_MEM -server -cp $JOSHUA/ext/berkeleylm/jar/berkeleylm.jar edu.berkeley.nlp.lm.io.MakeKneserNeyArpaFromText $LM_ORDER lm.gz $TRAIN{target}.uniq",
                    "$lm_input",
										$lmfile);
  } else {
    # Make sure it exists
    if (! -e "$JOSHUA/bin/lmplz") {
      print "* FATAL: $JOSHUA/bin/lmplz (for building LMs) does not exist.\n";
      print "  This is often a problem with the boost libraries (particularly threaded\n";
      print "  versus unthreaded).\n";
      exit 1;
    }

    # Needs to be capitalized
    my $mem = uc $BUILDLM_MEM;
    $cachepipe->cmd("kenlm",
                    "$JOSHUA/bin/lmplz -o $LM_ORDER -T $TMPDIR -S $mem --verbose_header --text $TRAIN{target}.uniq $LM_OPTIONS | gzip -9n > lm.gz",
                    "$TRAIN{target}.uniq",
                    $lmfile);
  }

  if ((! $MERGE_LMS) && ($LM_TYPE eq "kenlm" || $LM_TYPE eq "berkeleylm")) {
    push (@LMFILES, get_absolute_path(compile_lm $lmfile, $RUNDIR));
  } else {
    push (@LMFILES, get_absolute_path($lmfile, $RUNDIR));
  }
}

if ($DO_BUILD_CLASS_LM) {
  # Build a Class LM
  # First check to see if an class map and class corpus are defined
  if (! defined $CLASS_LM_CORPUS or ! defined $CLASS_MAP) {
    print "* FATAL: A class LM corpus (--class-lm-corpus) and a class map (--class-map) are required with the --class-lm switch";
    exit 1;
  }
  if (! -e $CLASS_LM_CORPUS or ! -e $CLASS_MAP) {
    print "* FATAL: Could not find the Class LM corpus or map";
    exit 1;
  }
  if (! -e "$JOSHUA/bin/lmplz") {
    print "* FATAL: $JOSHUA/bin/lmplz (for building LMs) does not exist.\n";
    print "  This is often a problem with the boost libraries (particularly threaded\n";
    print "  versus unthreaded).\n";
    exit 1;
  }

  # Needs to be capitalized
  my $mem = uc $BUILDLM_MEM;
  my $class_lmfile = "class_lm.gz";
  $cachepipe->cmd("classlm",
                  "$JOSHUA/bin/lmplz -o $CLASS_LM_ORDER -T $TMPDIR -S $mem --discount_fallback=0.5 1 1.5 --verbose_header --text $CLASS_LM_CORPUS $LM_OPTIONS | gzip -9n > $class_lmfile",
                  "$CLASS_LM_CORPUS",
                  $class_lmfile);
}

if ($MERGE_LMS) {
  # Merge @LMFILES.
  my $merged_lm = "lm-merged.gz";

  # Use the target first target reference if there are multiple ones
  my $target_ref = (-e $TUNE{target}) ? $TUNE{target} : "$TUNE{target}.0";

  $cachepipe->cmd("merge-lms",
                  "$JOSHUA/scripts/support/merge_lms.py "
                    . "@LMFILES "
                    . "$target_ref "
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

# Set $TUNE_GRAMMAR to a specifically-passed tuning grammar or the
# main default grammar. Then update it if filtering was requested and
# is possible.
my $TUNE_GRAMMAR = $_TUNE_GRAMMAR_FILE || $GRAMMAR_FILE;
if ($DO_FILTER_TM and defined $GRAMMAR_FILE and ! $DOING_LATTICES and ! defined $_TUNE_GRAMMAR_FILE) {
  $TUNE_GRAMMAR = "$DATA_DIRS{tune}/grammar.filtered.gz";

  if ($OPTIMIZER_RUN == 1 and ! is_packed($TUNE_GRAMMAR)) {
    $cachepipe->cmd("filter-tune",
                    "$SCRIPTDIR/support/filter_grammar.sh -g $GRAMMAR_FILE $FILTERING -v $TUNE{source} | $SCRIPTDIR/training/filter-rules.pl -bus$SCOPE | gzip -9n > $TUNE_GRAMMAR",
                    $GRAMMAR_FILE,
                    $TUNE{source},
                    "$DATA_DIRS{tune}/grammar.filtered.gz");
  }
}

# Create the glue grammars. This is done by looking at all the symbols in the grammar file and
# creating all the needed rules. This is only done if there is a $TUNE_GRAMMAR defined (which
# can be skipped if we skip straight to the tuning step).
if ($OPTIMIZER_RUN == 1 and defined $TUNE_GRAMMAR and $GRAMMAR_TYPE ne "phrase" and $GRAMMAR_TYPE ne "moses") {
  if (! defined $GLUE_GRAMMAR_FILE) {
    $cachepipe->cmd("glue-tune",
                    "java -Xmx2g -cp $JOSHUA/lib/args4j-2.0.29.jar:$JOSHUA/class joshua.decoder.ff.tm.CreateGlueGrammar -g $TUNE_GRAMMAR > $DATA_DIRS{tune}/grammar.glue",
                    get_file_from_grammar($TUNE_GRAMMAR),
                    "$DATA_DIRS{tune}/grammar.glue");
    $GLUE_GRAMMAR_FILE = "$DATA_DIRS{tune}/grammar.glue";
  } else {
    # just create a symlink to it
    my $filename = $DATA_DIRS{tune} . "/" . basename($GLUE_GRAMMAR_FILE);
    system("ln -sf $GLUE_GRAMMAR_FILE $filename");
  }
}

# Add in feature functions
my $weightstr = "";
my @feature_functions;
my $lm_index = 0;
for my $i (0..$#LMFILES) {
  if ($LM_STATE_MINIMIZATION) {
    push(@feature_functions, "StateMinimizingLanguageModel -lm_order $LM_ORDER -lm_file $LMFILES[$i]");
  } else {
    push(@feature_functions, "LanguageModel -lm_type $LM_TYPE -lm_order $LM_ORDER -lm_file $LMFILES[$i]");
  }

  $weightstr .= "lm_$i 1 ";
  $lm_index += 1;
}

if ($DO_BUILD_CLASS_LM) {
  push(@feature_functions, "LanguageModel -lm_type kenlm -lm_order 9 -lm_file $RUNDIR/class_lm.gz -class_map $CLASS_MAP");
  $weightstr .= "lm_$lm_index 1 ";
}

if ($DOING_LATTICES) {
  push(@feature_functions, "SourcePath");

  $weightstr .= "SourcePath 1.0 ";
}
if ($GRAMMAR_TYPE eq "phrase" or $GRAMMAR_TYPE eq "moses") {
  push(@feature_functions, "Distortion");
  push(@feature_functions, "PhrasePenalty");

  $weightstr .= "Distortion 1.0 PhrasePenalty 1.0 ";
}
my $feature_functions = join(" ", map { "-feature-function \"$_\"" } @feature_functions);

# Build out the weight string
my $TM_OWNER = "pt";
my $GLUE_OWNER = "glue";
if (defined $TUNE_GRAMMAR) {
  my @tm_features = get_features($TUNE_GRAMMAR);
  foreach my $feature (@tm_features) {
    # Only assign initial weights to dense features
    $weightstr .= "tm_${TM_OWNER}_$feature 1 " if ($feature =~ /^\d+$/);
  }

  # Glue grammars are only needed for hierarchical models
  if ($GRAMMAR_TYPE ne "phrase" and $GRAMMAR_TYPE ne "moses") {
    # Glue grammar
    $weightstr .= "tm_${GLUE_OWNER}_0 1 ";
  }
}

my $tm_type = $GRAMMAR_TYPE;
if ($GRAMMAR_TYPE eq "moses") {
  $tm_type = "moses";
}

sub get_file_from_grammar {
  # Cachepipe doesn't work on directories, so we need to make sure we
  # have a representative file to use to cache grammars. Returns undef if file not found
  my ($grammar_file) = @_;
  return undef unless defined $grammar_file and -e $grammar_file;
  my $file = (-d $grammar_file) ? "$grammar_file/slice_00000.source" : $grammar_file;
  return $file;
}

# The first tuning run is just a symlink to the tune/ directory (for backward compat.)
# Subsequent runs are under their run number
my $tunedir;
if ($OPTIMIZER_RUN == 1) {
  $tunedir = "$RUNDIR/tune";
  system("mkdir -p $tunedir") unless -d $tunedir;
  symlink "$RUNDIR/tune", "$RUNDIR/tune/1";
} else {
  $tunedir = "$RUNDIR/tune/$OPTIMIZER_RUN";
  system("mkdir -p $tunedir") unless -d $tunedir;
}

system("mkdir -p $tunedir") unless -d $tunedir;

# Build the filtered tuning model
my $tunemodeldir = "$RUNDIR/tune/model";

# We build up this string with TMs to substitute in, if any are provided
my $tm_switch = "";
my $tm_copy_config_args = "";
if (defined $TUNE_GRAMMAR) {
  $tm_switch .= ($DO_PACK_GRAMMARS) ? "--pack-tm" : "--tm";
  $tm_switch .= " $TUNE_GRAMMAR";
  $tm_copy_config_args = " -tm0/type $tm_type -tm0/owner ${TM_OWNER} -tm0/maxspan $MAXSPAN";
}
# If we specified a new glue grammar, put that in
if ($GRAMMAR_TYPE eq "phrase" or $GRAMMAR_TYPE eq "moses") {
  # if there is no glue grammar, remove it from the config template
  $tm_copy_config_args .= " -tm1 DELETE";
} elsif (defined $GLUE_GRAMMAR_FILE) {
  $tm_switch .= " --tm $GLUE_GRAMMAR_FILE";
  $tm_copy_config_args .= " -tm1/owner ${GLUE_OWNER}";
}

# Now build the bundle
if ($OPTIMIZER_RUN == 1) {
  $cachepipe->cmd("tune-bundle",
                  "$BUNDLER --force --symlink --absolute --verbose -T $TMPDIR $JOSHUA_CONFIG $tunemodeldir --copy-config-options '-top-n $NBEST -output-format \"%i ||| %s ||| %f ||| %c\" -mark-oovs false -search $SEARCH_ALGORITHM -weights \"$weightstr\" $feature_functions ${tm_copy_config_args}' ${tm_switch}",
                  $JOSHUA_CONFIG,
                  get_file_from_grammar($TUNE_GRAMMAR) || $JOSHUA_CONFIG,
                  "$tunemodeldir/run-joshua.sh");
}

# Update the tune grammar to its new location in the bundle
if (defined $TUNE_GRAMMAR) {
  # Now update the tuning grammar to its new path
  my $basename = basename($TUNE_GRAMMAR);
  if (-e "tune/model/$basename") {
    $TUNE_GRAMMAR = "tune/model/$basename";
  } elsif (-e "tune/model/$basename.packed") {
    $TUNE_GRAMMAR = "tune/model/$basename.packed";
  } else {
    print STDERR "* FATAL: tune model bundling didn't produce a grammar?";
    exit 1;
  }
}

# Copy the generated config to the tunedir, and update the config file location
system("cp $tunemodeldir/joshua.config $tunedir/joshua.config");
$JOSHUA_CONFIG = "$tunedir/joshua.config";

# Write the decoder run command. The decoder will use the config file in the bundled
# directory, continually updating it.

# If we're decoding a lattice, also output the source side path we chose
$JOSHUA_ARGS = "";
if ($DOING_LATTICES) {
  $JOSHUA_ARGS .= " -maxlen 0 -lattice-decoding";
}
$JOSHUA_ARGS .= " -output-format \"%i ||| %s ||| %f ||| %c\"";
$JOSHUA_ARGS .= " $_JOSHUA_ARGS" if defined $_JOSHUA_ARGS;

open DEC_CMD, ">$tunedir/decoder_command";
print DEC_CMD "cat $TUNE{source} | $tunemodeldir/run-joshua.sh -m $JOSHUA_MEM -config $JOSHUA_CONFIG -threads $NUM_THREADS $JOSHUA_ARGS > $tunedir/output.nbest 2> $tunedir/joshua.log\n";
close(DEC_CMD);
chmod(0755,"$tunedir/decoder_command");

# tune
if ($TUNER ne "kbmira") {
  $cachepipe->cmd("${TUNER}-${OPTIMIZER_RUN}",
                  "$SCRIPTDIR/training/run_tuner.py $TUNE{source} $TUNE{target} --tunedir $tunedir --tuner $TUNER --decoder $tunedir/decoder_command --decoder-config $JOSHUA_CONFIG --decoder-output-file $tunedir/output.nbest --decoder-log-file $tunedir/joshua.log --iterations $TUNER_ITERATIONS --metric '$METRIC'",
                  $TUNE{source},
                  $JOSHUA_CONFIG,
                  get_file_from_grammar($TUNE_GRAMMAR) || $JOSHUA_CONFIG,
                  "$tunedir/joshua.config.final");

} else { # Moses' batch kbmira
  my $refs_path = $TUNE{target};
  $refs_path .= "." if (get_numrefs($TUNE{target}) > 1);

  my $extra_args = $JOSHUA_ARGS;
  $extra_args =~ s/"/\\"/g;
  $cachepipe->cmd("kbmira-${OPTIMIZER_RUN}",
                  "$SCRIPTDIR/training/mira/run-mira.pl --mertdir $MOSES/bin --rootdir $MOSES/scripts --batch-mira --working-dir $tunedir --maximum-iterations $TUNER_ITERATIONS --nbest $NBEST --no-filter-phrase-table --decoder-flags \"-m $JOSHUA_MEM -threads $NUM_THREADS -moses $extra_args\" $TUNE{source} $refs_path $tunemodeldir/run-joshua.sh $JOSHUA_CONFIG > $tunedir/mira.log 2>&1",
                  get_file_from_grammar($TUNE_GRAMMAR) || $JOSHUA_CONFIG,
                  $TUNE{source},
                  "$tunedir/joshua.config.final");
}

$JOSHUA_CONFIG = "$tunedir/joshua.config.final";

# Go to the next tuning run if tuning is the last step.
maybe_quit("TUNE");

#################################################################
## TESTING ######################################################
#################################################################

TEST:
    ;

# prepare the testing data
if (! $PREPPED{TEST} and $OPTIMIZER_RUN == 1) {
  my $prefixes = prepare_data("test", [$TEST], $MAXLEN_TEST);
  $TEST{source} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$SOURCE";
  $TEST{target} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TEST} = 1;
}

system("mkdir -p $DATA_DIRS{test}") unless -d $DATA_DIRS{test};

# Define the test grammar, if it was provided
my $TEST_GRAMMAR = $_TEST_GRAMMAR_FILE || $GRAMMAR_FILE;

if ($DO_FILTER_TM and defined $GRAMMAR_FILE and ! $DOING_LATTICES and ! defined $_TEST_GRAMMAR_FILE) {
  # On the first test run, we take some pains to prepare and pack the model, which won't have
  # to be done for subsequent runs
  if ($OPTIMIZER_RUN == 1 and ! is_packed($TEST_GRAMMAR)) {
    $TEST_GRAMMAR = "$DATA_DIRS{test}/grammar.filtered.gz";

    $cachepipe->cmd("filter-test",
                    "$SCRIPTDIR/support/filter_grammar.sh -g $GRAMMAR_FILE $FILTERING -v $TEST{source} | $SCRIPTDIR/training/filter-rules.pl -bus$SCOPE | gzip -9n > $TEST_GRAMMAR",
                    $GRAMMAR_FILE,
                    $TEST{source},
                    "$DATA_DIRS{test}/grammar.filtered.gz");
  }
}

# Create the glue grammar
if ($OPTIMIZER_RUN == 1 and defined $TEST_GRAMMAR and $GRAMMAR_TYPE ne "phrase" and $GRAMMAR_TYPE ne "moses") {
  if (! defined $GLUE_GRAMMAR_FILE) {
    $cachepipe->cmd("glue-test",
                    "java -Xmx2g -cp $JOSHUA/lib/args4j-2.0.29.jar:$JOSHUA/class joshua.decoder.ff.tm.CreateGlueGrammar -g $TEST_GRAMMAR > $DATA_DIRS{test}/grammar.glue",
                    get_file_from_grammar($TEST_GRAMMAR),
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
}

# Create the test directory
my $testdir;
if ($OPTIMIZER_RUN == 1) {
  $testdir = "$RUNDIR/test";
  system("mkdir -p $testdir") unless -d $testdir;
  symlink("$RUNDIR/test", "$RUNDIR/test/1");
} else {
  $testdir = "$RUNDIR/test/$OPTIMIZER_RUN";
  system("mkdir -p $testdir") unless -d $testdir;
}

$tm_switch = "";
$tm_copy_config_args = "";
if ($DO_PACK_GRAMMARS) {
  my $packed_dir = "$DATA_DIRS{test}/grammar.packed";
  if ($OPTIMIZER_RUN == 1 and ! is_packed($TEST_GRAMMAR)) {
    $cachepipe->cmd("test-pack",
                    "$SCRIPTDIR/support/grammar-packer.pl -T $TMPDIR -m $PACKER_MEM -g $TEST_GRAMMAR -o $packed_dir",
                    $TEST_GRAMMAR,
                    "$packed_dir/vocabulary",
                    "$packed_dir/encoding",
                    "$packed_dir/slice_00000.source");
  }
  $TEST_GRAMMAR = $packed_dir;

  $tm_switch .= " --pack-tm $TEST_GRAMMAR";
} else {
  $tm_switch .= " --tm $TEST_GRAMMAR";
}

# Add in the glue grammar
if (defined $GLUE_GRAMMAR_FILE) {
  $tm_switch .= " --tm $GLUE_GRAMMAR_FILE";
}

# Build the test model
my $testmodeldir = "$RUNDIR/test/$OPTIMIZER_RUN/model";
$cachepipe->cmd("test-bundle-${OPTIMIZER_RUN}",
                "$BUNDLER --force --symlink --absolute --verbose -T $TMPDIR $JOSHUA_CONFIG $testmodeldir --copy-config-options '-top-n $NBEST -pop-limit 5000 -output-format \"%i ||| %s ||| %f ||| %c\" -mark-oovs false' ${tm_switch}",
                $JOSHUA_CONFIG,
                get_file_from_grammar($TEST_GRAMMAR) || $JOSHUA_CONFIG,
                "$testmodeldir/joshua.config");

if (defined $TEST_GRAMMAR) {
  # Update the test grammar (if defined) to its new path
  my $basename = basename($TEST_GRAMMAR);
  if (-e "$testmodeldir/$basename") {
    $TEST_GRAMMAR = "$testmodeldir/$basename";
  } elsif (-e "$testmodeldir/$basename.packed") {
    $TEST_GRAMMAR = "$testmodeldir/$basename.packed";
  } else {
    print STDERR "* FATAL: test model bundling didn't produce a grammar?";
    exit 1;
  }
}

my $bestoutput = "$testdir/output";
my $nbestoutput = "$testdir/output.nbest";
my $output;

# If we're decoding a lattice, also output the source side path we chose
$JOSHUA_ARGS = "";
if ($DOING_LATTICES) {
  $JOSHUA_ARGS .= " -maxlen 0 -lattice-decoding -output-format \"%i ||| %s ||| %e ||| %f ||| %c\"";
}

if ($DO_MBR) {
  $JOSHUA_ARGS .= " -top-n $NBEST -output-format \"%i ||| %s ||| %f ||| %c\"";
  $output = $nbestoutput;
} else {
  $JOSHUA_ARGS .= " -top-n 0 -output-format %s";
  $output = $bestoutput;
}
$JOSHUA_ARGS .= " $_JOSHUA_ARGS" if defined $_JOSHUA_ARGS;

# Write the decoder run command
open DEC_CMD, ">$testdir/decoder_command";
print DEC_CMD "cat $TEST{source} | $testmodeldir/run-joshua.sh -m $JOSHUA_MEM -threads $NUM_THREADS $JOSHUA_ARGS > $output 2> $testdir/joshua.log\n";
close(DEC_CMD);
chmod(0755,"$testdir/decoder_command");

# Decode. $output here is either $nbestoutput (if doing MBR decoding, in which case we'll
# need the n-best output) or $bestoutput (which only outputs the hypothesis but is tons faster)
$cachepipe->cmd("test-decode-${OPTIMIZER_RUN}",
                "$testdir/decoder_command",
                $TEST{source},
                "$testdir/decoder_command",
                "$testmodeldir/joshua.config",
                get_file_from_grammar($TEST_GRAMMAR) || "$testmodeldir/joshua.config",
                $output);

# $cachepipe->cmd("remove-oov",
#                 "cat $testoutput | perl -pe 's/_OOV//g' > $testoutput.noOOV",
#                 $testoutput,
#                 "$testoutput.noOOV");

# Extract the 1-best output from the n-best file if the n-best file alone was output
if ($DO_MBR) {
  $cachepipe->cmd("test-extract-onebest-${OPTIMIZER_RUN}",
                  "java -Xmx500m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.util.ExtractTopCand $nbestoutput $bestoutput",
                  $nbestoutput,
                  $bestoutput);
}  

# Now compute the BLEU score on the 1-best output
$cachepipe->cmd("test-bleu-${OPTIMIZER_RUN}",
                "$JOSHUA/bin/bleu $output $TEST{target} > $testdir/bleu",
                $bestoutput,
                "$testdir/bleu");

# Update the BLEU summary.
compute_bleu_summary("test/*/bleu", "test/final-bleu");

if (defined $METEOR) {
  $cachepipe->cmd("test-meteor-${OPTIMIZER_RUN}",
                  "$JOSHUA/bin/meteor $output $TEST{target} $TARGET > $testdir/meteor",
                  $bestoutput,
                  "$testdir/meteor");
  compute_meteor_summary("test/*/meteor", "test/final-meteor");
}

if ($DO_MBR) {
  my $numlines = `cat $TEST{source} | wc -l`;
  $numlines--;
  my $mbr_output = "$testdir/output.mbr";

  $cachepipe->cmd("test-onebest-parmbr-${OPTIMIZER_RUN}", 
                  "cat $nbestoutput | java -Xmx1700m -cp $JOSHUA/class -Dfile.encoding=utf8 joshua.decoder.NbestMinRiskReranker false 1 $NUM_THREADS > $mbr_output",
                  $nbestoutput,
                  $mbr_output);

  $cachepipe->cmd("test-bleu-mbr-${OPTIMIZER_RUN}",
                  "$JOSHUA/bin/bleu output $TEST{target} $numrefs > $testdir/bleu.mbr",
                  $mbr_output,
                  "$testdir/bleu.mbr");

  compute_bleu_summary("test/*/bleu.mbr", "test/final-bleu-mbr");
}

compute_time_summary("test/*/joshua.log", "test/final-times");

# Now do the analysis
if ($DOING_LATTICES) {
  # extract the source
  my $source = "$testdir/test.lattice-path.txt";
  $cachepipe->cmd("test-lattice-extract-source-${OPTIMIZER_RUN}",
                  "$JOSHUA/bin/extract-1best $nbestoutput 2 | perl -pe 's/<s> //' > $source",
                  $nbestoutput, $source);

  analyze_testrun($bestoutput,$source,$TEST{target});
} else {
  analyze_testrun($bestoutput,$TEST{source},$TEST{target});
}


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
  
  # Build the list of extensions. For training data, there may be multiple corpora; for
  # tuning and test data, there may be multiple references.
  my @exts = ($SOURCE);
  my $target_corpus = "$corpora->[0].$TARGET";
  push(@exts, $TARGET) if -e $target_corpus;
  for (my $i = 0; ; $i++) {
    my $file = "$target_corpus.$i";
    if (-e $file) {
      push(@exts, "$TARGET.$i");
    } else {
      last;
    }
  }

  # Read through all input files, concatenate them (if multiple were passed), and filter them
  # First, assemble the file handles
  my (@infiles, @indeps, @outfiles);
  foreach my $ext (@exts) {
    my @files =  map { "$_.$ext" } @$corpora;
    push(@indeps, @files);
    if (@files > 1) {
      push(@infiles, "<(cat " . join(" ", @files) . ")");
    } else {
      push(@infiles, $files[0]);
    }
    push (@outfiles, "$DATA_DIRS{$label}/$label.$ext");
  }

  my $infiles =  join(" ", @infiles);
  my $outfiles = join(" ", @outfiles);
  # only skip blank lines for training data
  if ($label eq "train") {
    $cachepipe->cmd("$label-copy-and-filter",
                    "$PASTE $infiles | $SCRIPTDIR/training/filter-empty-lines.pl | $SCRIPTDIR/training/split2files.pl $outfiles",
                    @indeps, @outfiles);
  } else {
    $cachepipe->cmd("$label-copy-and-filter",
                    "$PASTE $infiles | $SCRIPTDIR/training/split2files.pl $outfiles",
                    @indeps, @outfiles);
  }
  # Done concatenating and filtering files

  # record where the concatenated input files were
  $prefixes{last_step} = $prefixes{input} = "$DATA_DIRS{$label}/$label";

  if ($DO_PREPARE_CORPORA) {
    my $prefix = $label;

    # tokenize the data
    foreach my $lang (@exts) {
      if (-e "$DATA_DIRS{$label}/$prefix.$lang") {
        if (is_lattice("$DATA_DIRS{$label}/$prefix.$lang")) { 
          system("cp $DATA_DIRS{$label}/$prefix.$lang $DATA_DIRS{$label}/$prefix.tok.$lang");
        } else {
          my $TOKENIZER = ($lang eq $SOURCE) ? $TOKENIZER_SOURCE : $TOKENIZER_TARGET;
          my $ext = $lang; $ext =~ s/\.\d//;
          $cachepipe->cmd("$label-tokenize-$lang",
                          "$CAT $DATA_DIRS{$label}/$prefix.$lang | $NORMALIZER $ext | $TOKENIZER -l $ext 2> /dev/null > $DATA_DIRS{$label}/$prefix.tok.$lang",
                          "$DATA_DIRS{$label}/$prefix.$lang", "$DATA_DIRS{$label}/$prefix.tok.$lang");
        }

      }
    }
    # extend the prefix
    $prefix .= ".tok";
    $prefixes{tokenized} = $prefix;

    if ($maxlen > 0) {
      my (@infiles, @outfiles);
      foreach my $ext (@exts) {
        my $infile = "$DATA_DIRS{$label}/$prefix.$ext";
        my $outfile = "$DATA_DIRS{$label}/$prefix.$maxlen.$ext";
        if (-e $infile) {
          push(@infiles, $infile);
          push(@outfiles, $outfile);
        }
      }

      my $infilelist = join(" ", @infiles);
      my $outfilelist = join(" ", @outfiles);

      # trim training data
      $cachepipe->cmd("$label-trim",
                      "$PASTE $infilelist | $SCRIPTDIR/training/trim_parallel_corpus.pl $maxlen | $SCRIPTDIR/training/split2files.pl $outfilelist",
                      @infiles,
                      @outfiles);
      $prefix .= ".$maxlen";
    }
    # record this whether we shortened or not
    $prefixes{shortened} = $prefix;

    # lowercase
    foreach my $lang (@exts) {
      if (-e "$DATA_DIRS{$label}/$prefix.$lang") {
        if (is_lattice("$DATA_DIRS{$label}/$prefix.$lang")) { 
          system("cat $DATA_DIRS{$label}/$prefix.$lang > $DATA_DIRS{$label}/$prefix.lc.$lang");
        } else { 
          $cachepipe->cmd("$label-lowercase-$lang",
                          "cat $DATA_DIRS{$label}/$prefix.$lang | $LOWERCASER > $DATA_DIRS{$label}/$prefix.lc.$lang",
                          "$DATA_DIRS{$label}/$prefix.$lang",
                          "$DATA_DIRS{$label}/$prefix.lc.$lang");
        }
      }
    }
    $prefix .= ".lc";
    $prefixes{last_step} = $prefixes{lowercased} = $prefix;
  }

  foreach my $lang (@exts) {
    system("ln -sf $prefixes{last_step}.$lang $DATA_DIRS{$label}/corpus.$lang");
  }

  # Build a vocabulary
  foreach my $ext (@exts) {
    $cachepipe->cmd("$label-vocab-$ext",
                    "cat $DATA_DIRS{$label}/corpus.$ext | $SCRIPTDIR/training/build-vocab.pl > $DATA_DIRS{$label}/vocab.$ext",
                    "$DATA_DIRS{$label}/corpus.$ext",
                    "$DATA_DIRS{$label}/vocab.$ext");
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

    my $hadoop_tmp_dir = tempdir("hadoop-XXXX", DIR => $TMPDIR, CLEANUP => 0);
		system("tar xzf $JOSHUA/lib/hadoop-2.5.2.tar.gz -C $hadoop_tmp_dir");
		system("ln -sf $hadoop_tmp_dir/hadoop-2.5.2 hadoop");
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

#sub teardown_hadoop_cluster {
#  stop_hadoop_cluster();
#  system("rm -f hadoop");
#}

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

# Set membership: is value in array?
sub in {
  my ($value, $array) = @_;
  return grep( /^$value$/, @$array );
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
      # field 4 for regular grammars, field 3 for phrase tables
      my $feature_str = ($line =~ /^\[/) ? $tokens[3] : $tokens[2];
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

  $cachepipe->cmd("analyze-test-${OPTIMIZER_RUN}",
                  "$SCRIPTDIR/analysis/sentence-by-sentence.pl -s $source -r $references $output > $dir/analysis/sentence-by-sentence.html",
                  $output,
                  "$dir/analysis/sentence-by-sentence.html");
}

sub compute_meteor_summary {
  my ($filepattern, $outputfile) = @_;

  # Average the runs, report result
  my @scores;
  my $numrecs = 0;
  open CMD, "grep '^Final score' $filepattern |";
  my @F = split(' ', <CMD>);
  close(CMD);
  push(@scores, 1.0 * $F[-1]);

  if (scalar @scores) {
    my $final_score = sum(@scores) / (scalar @scores);

    open SUMMARY, ">$outputfile" or die "Can't write to $outputfile";
    printf(SUMMARY "%s / %d = %.4f\n", join(" + ", @scores), scalar @scores, $final_score);
    close(SUMMARY);
  }
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
      next unless /^Input \d+: Translation took/;
      my @F = split;
      $time += $F[4];
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

sub ner_annotate {
  my ($inputfile, $outputfile, $lang) = @_;
  if (defined $NER_TAGGER) {
    # Check if NER tagger exists
    if (! -e $NER_TAGGER) {
      print "* FATAL: The specified NER tagger was not found";
      exit(1);
    }
    $cachepipe->cmd("ner-annotate", "$NER_TAGGER $inputfile $outputfile $lang");
    # Check if annotated file exists
    if (! -e "$outputfile") {
      print "* FATAL : The NER tagger did not create the required annotated file : $outputfile";
      exit(1);
    }
    return 2;
  }
  return 0;
}

sub replace_tokens_with_types {
  # Replace the tokens with types
  my ($inputfile) = @_;
  qx{sed -ir 's:\$([A-Za-z0-9]+)_\([^)]+\):\1:g' $inputfile}
}
