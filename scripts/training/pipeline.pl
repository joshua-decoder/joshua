#!/usr/bin/perl

# This script implements the Joshua pipeline.  It can run a complete
# pipeline --- from raw training corpora to bleu scores on a test set
# --- and it allows jumping into arbitrary points of the pipeline. 

my $JOSHUA;
my $HADOOP;

BEGIN {
  if (! exists $ENV{JOSHUA} || $ENV{JOSHUA} eq "" ||
      ! exists $ENV{HADOOP} || $ENV{HADOOP} eq "" ||
      ! exists $ENV{JAVA_HOME} || $ENV{JAVA_HOME} eq "") {
                print "Several environment variables must be set before running the pipeline.  Please set:\n";
                print "* \$JOSHUA to the root of the Joshua source code.\n"
                                if (! exists $ENV{JOSHUA} || $ENV{JOSHUA} eq "");
                print "* \$HADOOP to the directory of your local hadoop installation.\n"
                                if (! exists $ENV{HADOOP} || $ENV{HADOOP} eq "");
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
use Cwd;
use POSIX qw[ceil];
use List::Util qw[max min sum];
use File::Temp qw/ :mktemp /;
use CachePipe;
# use Thread::Pool;

$HADOOP = $ENV{HADOOP};

my $THRAX = "$JOSHUA/thrax";

die not_defined("JAVA_HOME") unless exists $ENV{JAVA_HOME};

my (@CORPORA,$TUNE,$TEST,$ALIGNMENT,$SOURCE,$TARGET,@LMFILES,$GRAMMAR_FILE,$GLUE_GRAMMAR_FILE,$TUNE_GRAMMAR_FILE,$TEST_GRAMMAR_FILE,$THRAX_CONF_FILE);
my $FIRST_STEP = "FIRST";
my $LAST_STEP  = "LAST";
my $LMFILTER = "$ENV{HOME}/code/filter/filter";
my $MAXLEN = 50;
my $DO_FILTER_TM = 1;
my $DO_SUBSAMPLE = 0;
my $SCRIPTDIR = "$JOSHUA/scripts";
my $TOKENIZER = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $NORMALIZER = "$SCRIPTDIR/training/normalize-punctuation.pl";
my $GIZA_TRAINER = "$SCRIPTDIR/training/run-giza.pl";
my $TUNECONFDIR = "$SCRIPTDIR/training/templates/tune";
my $SRILM = ($ENV{SRILM}||"")."/bin/i686-m64/ngram-count";
my $COPY_CONFIG = "$SCRIPTDIR/copy-config.pl";
my $STARTDIR;
my $RUNDIR = $STARTDIR = getcwd;
my $GRAMMAR_TYPE = "hiero";  # or "phrasal" or "samt"
my $WITTEN_BELL = 0;

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

my $DO_MBR = 1;

my $ALIGNER = "giza"; # "berkeley" or "giza"

# This is the amount of memory made available to Joshua.  You'll need
# a lot more than this for SAMT decoding (though really it depends
# mostly on your grammar size)
my $JOSHUA_MEM = "3100m";

# the amount of memory available for hadoop processes (passed to
# Hadoop via -Dmapred.child.java.opts
my $HADOOP_MEM = "2g";

# memory available to the parser
my $PARSER_MEM = "2g";

# memory available for building the language model
my $BUILDLM_MEM = "2g";

# When qsub is called for decoding, these arguments should be passed to it.
my $QSUB_ARGS  = "";

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
my $LM_GEN = "berkeleylm";

my @STEPS = qw[FIRST SUBSAMPLE ALIGN PARSE THRAX TUNE MERT PRO TEST LAST];
my %STEPS = map { $STEPS[$_] => $_ + 1 } (0..$#STEPS);

my $NAME = undef;

# Methods to use for merging alignments (see Koehn et al., 2003).
# Options are union, {intersect, grow, srctotgt, tgttosrc}-{diag,final,final-and,diag-final,diag-final-and}
my $GIZA_MERGE = "grow-diag-final";

# Which tuner to use by default
my $TUNER = "mert";  # or PRO

# location of already-parsed corpus
my $PARSED_CORPUS = undef;

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
  "source=s"          => \$SOURCE,
  "target=s"         => \$TARGET,
  "rundir=s"        => \$RUNDIR,
  "filter-tm!"        => \$DO_FILTER_TM,
  "lm=s"              => \$LM_TYPE,
  "lmfile=s"        => \@LMFILES,
  "lm-gen=s"          => \$LM_GEN,
  "corpus-lm!"        => \$DO_BUILD_LM_FROM_CORPUS,
  "witten-bell!"     => \$WITTEN_BELL,
  "tune-grammar=s"    => \$TUNE_GRAMMAR_FILE,
  "test-grammar=s"    => \$TEST_GRAMMAR_FILE,
  "grammar=s"        => \$GRAMMAR_FILE,
  "glue-grammar=s"     => \$GLUE_GRAMMAR_FILE,
  "mbr!"              => \$DO_MBR,
  "type=s"           => \$GRAMMAR_TYPE,
  "maxlen=i"        => \$MAXLEN,
  "tokenizer=s"      => \$TOKENIZER,
  "joshua-config=s"   => \$TUNEFILES{'joshua.config'},
  "joshua-mem=s"      => \$JOSHUA_MEM,
  "hadoop-mem=s"      => \$HADOOP_MEM,
  "parser-mem=s"      => \$PARSER_MEM,
  "buildlm-mem=s"     => \$BUILDLM_MEM,
  "decoder-command=s" => \$TUNEFILES{'decoder_command'},
  "tuner=s"           => \$TUNER,
  "thrax=s"           => \$THRAX,
  "thrax-conf=s"      => \$THRAX_CONF_FILE,
  "jobs=i"            => \$NUM_JOBS,
  "threads=i"         => \$NUM_THREADS,
  "subsample!"       => \$DO_SUBSAMPLE,
  "qsub-args=s"      => \$QSUB_ARGS,
  "first-step=s"     => \$FIRST_STEP,
  "last-step=s"      => \$LAST_STEP,
  "aligner-chunk-size=s" => \$ALIGNER_BLOCKSIZE,
  "hadoop=s"          => \$HADOOP,
  "optimizer-runs=i"  => \$OPTIMIZER_RUNS,
);

if (! $retval) {
  print "Invalid usage, quitting\n";
  exit 1;
}

my $DOING_LATTICES = 0;

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

# make sure the LMs exist
foreach my $lmfile (@LMFILES) {
  if (! -e $lmfile) {
    print "* FATAL: couldn't find language model file '$lmfile'\n";
    exit 1;
  }
}

# If a language model was specified and no corpus was given to build another one from the target
# side of the training data (which could happen, for example, when starting at the tuning step with
# an existing LM), turn off building an LM from the corpus.  The user could have done this
# explicitly with --no-corpus-lm, but might have forgotten to, and we con't want to pester them with
# an error about easily-inferrable intentions.
if (scalar @LMFILES && ! scalar(@CORPORA)) {
  $DO_BUILD_LM_FROM_CORPUS = 0;
}

# absolutize LM file paths
map {
  $LMFILES[$_] = get_absolute_path($LMFILES[$_]);
} 0..$#LMFILES;

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

foreach my $corpus (@CORPORA) {
  foreach my $ext ($TARGET,$SOURCE) {
    if (! -e "$corpus.$ext") {
      print "* FATAL: can't find '$corpus.$ext'";
      exit 1;
    } 
  }
}

if ($ALIGNER ne "giza" and $ALIGNER ne "berkeley") {
  print "* FATAL: aligner must be one of 'giza', or 'berkeley'\n";
  exit 1;
}

if ($LM_TYPE ne "kenlm" and $LM_TYPE ne "berkeleylm") {
  print "* FATAL: lm type (--lm) must be one of 'kenlm' or 'berkeleylm'\n";
  exit 1;
}

if ($LM_GEN ne "berkeleylm" and $LM_GEN ne "srilm") {
  print "* FATAL: lm generating code (--lm-gen) must be one of 'berkeleylm' (default) or 'srilm'\n";
  exit 1;
}


## Dependent variable setting ########################################

# if parallelization is turned off, then use the sequential version of
# the decoder command
if ($NUM_JOBS == 1) {
  $TUNEFILES{'decoder_command'} = "$TUNECONFDIR/decoder_command.sequential";
}

my $OOV = ($GRAMMAR_TYPE eq "samt") ? "OOV" : "X";

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
  $TRAIN{parsed} = $PARSED_CORPUS;
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
  my $prefixes = prepare_data("tune",[$TUNE]);
  $TUNE{source} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TUNE} = 1;
}

if (defined $TEST and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("test",[$TEST]);
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

	my @aligned_files;
  for (my $chunkno = 0; $chunkno <= $lastchunk; $chunkno++) {

		# create the alignment subdirectory
		my $chunkdir = "alignments/$chunkno";
		system("mkdir","-p", $chunkdir);
    
		if ($ALIGNER eq "giza") {
			run_giza($chunkdir, $chunkno, $NUM_THREADS > 1);
			# $pool->enqueue(\&run_giza, $chunkdir, $chunkno, $NUM_THREADS > 1);

			push(@aligned_files, "alignments/$chunkno/model/aligned.grow-diag-final");
		} elsif ($ALIGNER eq "berkeley") {
			run_berkeley_aligner($chunkdir, $chunkno);
			# $pool->enqueue(\&run_berkeley_aligner, $chunkdir, $chunkno);

			push(@aligned_files, "alignments/$chunkno/training.align");
		}
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

if ($GRAMMAR_TYPE eq "samt") {

  # If the user passed in the already-parsed corpus, use that (after copying it into place)
  if (defined $TRAIN{parsed} && -e $TRAIN{parsed}) {
    # copy and adjust the location of the file to its canonical location
    system("cp $TRAIN{parsed} $DATA_DIRS{train}/corpus.parsed.$TARGET");
    $TRAIN{parsed} = "$DATA_DIRS{train}/corpus.parsed.$TARGET";
  } else {

    $cachepipe->cmd("build-vocab",
                    "cat $TRAIN{target} | $SCRIPTDIR/training/build-vocab.pl > $DATA_DIRS{train}/vocab.$TARGET",
                    $TRAIN{target},
                    "$DATA_DIRS{train}/vocab.$TARGET");

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
                      "$CAT $TRAIN{mixedcase} | $JOSHUA/scripts/training/parallelize/parallelize.pl --jobs $NUM_JOBS --qsub-args \"$QSUB_ARGS\" -p 8g -- java -d64 -Xmx${PARSER_MEM} -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr -nThreads 1 | sed 's/^\(/\(TOP/' | perl $SCRIPTDIR/training/add-OOVs.pl $DATA_DIRS{train}/vocab.$TARGET | tee $DATA_DIRS{train}/corpus.$TARGET.parsed | $SCRIPTDIR/training/lowercase-leaves.pl > $DATA_DIRS{train}/corpus.parsed.$TARGET",
                      "$TRAIN{target}",
                      "$DATA_DIRS{train}/corpus.parsed.$TARGET");
    } else {
      $cachepipe->cmd("parse",
                      "$CAT $TRAIN{mixedcase} | java -d64 -Xmx${PARSER_MEM} -jar $JOSHUA/lib/BerkeleyParser.jar -gr $JOSHUA/lib/eng_sm6.gr -nThreads $NUM_THREADS | sed 's/^\(/\(TOP/' | perl $SCRIPTDIR/training/add-OOVs.pl $DATA_DIRS{train}/vocab.$TARGET | tee $DATA_DIRS{train}/corpus.$TARGET.parsed | $SCRIPTDIR/training/lowercase-leaves.pl > $DATA_DIRS{train}/corpus.parsed.$TARGET",
                      "$TRAIN{target}",
                      "$DATA_DIRS{train}/corpus.parsed.$TARGET");
    }

    $TRAIN{parsed} = "$DATA_DIRS{train}/corpus.parsed.$TARGET";
  }
}

maybe_quit("PARSE");

## THRAX #############################################################

THRAX:
    ;

system("mkdir -p $DATA_DIRS{train}") unless -d $DATA_DIRS{train};

if ($GRAMMAR_TYPE eq "samt") {

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

  # Since this is an expensive step, we short-circuit it if the grammar file is present.  I'm not
  # sure that this is the right behavior.
  if (! -e "grammar.gz" && ! -z "grammar.gz") {

		# create the input file
		my $target_file = ($GRAMMAR_TYPE eq "samt") 
				? $TRAIN{parsed} : $TRAIN{target};
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
										"$HADOOP/bin/hadoop jar $THRAX/bin/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' $thrax_file $THRAXDIR > thrax.log 2>&1; rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $THRAXDIR/final/ grammar; $HADOOP/bin/hadoop fs -rmr $THRAXDIR; gzip -9nf grammar",
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

## TUNING ##############################################################
TUNE:
    ;

# prep the tuning data, unless already prepped
if (! $PREPPED{TUNE} and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("tune",[$TUNE]);
  $TUNE{source} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$SOURCE";
  $TUNE{target} = "$DATA_DIRS{tune}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TUNE} = 1;
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
  if ($LM_GEN eq "srilm") {
		my $smoothing = ($WITTEN_BELL) ? "-wbdiscount" : "-kndiscount";
		$cachepipe->cmd("srilm",
										"$SRILM -interpolate $smoothing -order $LM_ORDER -text $TRAIN{target} -unk -lm lm.gz",
										$lmfile);
  } else {
		$cachepipe->cmd("berkeleylm",
										"java -ea -mx$BUILDLM_MEM -server -cp $JOSHUA/lib/berkeleylm.jar edu.berkeley.nlp.lm.io.MakeKneserNeyArpaFromText $LM_ORDER lm.gz $TRAIN{target}",
										$lmfile);
  }

	if ($LM_TYPE eq "kenlm") {
		my $kenlm_file = "lm.kenlm";
		$cachepipe->cmd("compile-kenlm",
										"$JOSHUA/src/joshua/decoder/ff/lm/kenlm/build_binary lm.gz lm.kenlm",
										$lmfile, $kenlm_file);

		push (@LMFILES, $kenlm_file);

	} elsif ($LM_TYPE eq "berkeleylm") {
		my $berkeleylm_file = "lm.berkeleylm";
		$cachepipe->cmd("compile-berkeleylm",
										"java -cp $JOSHUA/lib/berkeleylm.jar -server -mx$BUILDLM_MEM edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa lm.gz lm.berkeleylm",
										$lmfile, $berkeleylm_file);

		push (@LMFILES, $berkeleylm_file);
	} else {

		push (@LMFILES, $lmfile);
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


# filter the tuning grammar
my $TUNE_GRAMMAR = (defined $TUNE_GRAMMAR_FILE)
		? $TUNE_GRAMMAR_FILE
		: $GRAMMAR_FILE;

if ($DO_FILTER_TM and ! defined $TUNE_GRAMMAR_FILE) {
  $TUNE_GRAMMAR = "$DATA_DIRS{tune}/grammar.filtered.gz";

  $cachepipe->cmd("filter-tune",
									"$CAT $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $THRAX/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $TUNE{source} | $SCRIPTDIR/training/remove-unary-abstract.pl | grep -av '|||  |||' | gzip -9n > $TUNE_GRAMMAR",
									$GRAMMAR_FILE,
									$TUNE{source},
									$TUNE_GRAMMAR);
}

# create the glue grammars
if (! defined $GLUE_GRAMMAR_FILE) {
  $cachepipe->cmd("glue-tune",
									"$CAT $TUNE_GRAMMAR | java -Xmx2g -cp $THRAX/bin/thrax.jar:$JOSHUA/lib/hadoop-core-0.20.203.0.jar:$JOSHUA/lib/commons-logging-1.1.1.jar edu.jhu.thrax.util.CreateGlueGrammar $THRAX_CONF_FILE > $DATA_DIRS{tune}/grammar.glue",
									$TUNE_GRAMMAR,
									"$DATA_DIRS{tune}/grammar.glue");
  $GLUE_GRAMMAR_FILE = "$DATA_DIRS{tune}/grammar.glue";
} else {
  # just create a symlink to it
  my $filename = $DATA_DIRS{tune} . "/" . basename($GLUE_GRAMMAR_FILE);
  system("ln -sf $GLUE_GRAMMAR_FILE $filename");
}

# For each language model, we need to create an entry in the Joshua
# config file and in ZMert's params.txt file.  We use %lm_strings to
# build the corresponding string substitutions
my (@configstrings, @weightstrings, @lmparamstrings);
for my $i (0..$#LMFILES) {
  my $lmfile = $LMFILES[$i];

  my $configstring = "lm = $LM_TYPE $LM_ORDER false false 100 $lmfile";
  push (@configstrings, $configstring);

  my $weightstring = "lm $i 1.0";
  push (@weightstrings, $weightstring);

  my $lmparamstring = "lm $i               |||     1.000000 Opt     0.1     +Inf    +0.5    +1.5";
  push (@lmparamstrings, $lmparamstring);
}

my $lmlines   = join($/, @configstrings);
my $lmweights = join($/, @weightstrings);
my $lmparams  = join($/, @lmparamstrings);

my $num_tm_features = count_num_features($TUNE_GRAMMAR);
my (@tmparamstrings, @tmweightstrings);
for my $i (0..($num_tm_features-1)) {
  push (@tmparamstrings, "phrasemodel pt $i |||  1.0 Opt -Inf +Inf -1 +1");
	push (@tmweightstrings, "phrasemodel pt $i 1.0");
}

my $tmparams = join($/, @tmparamstrings);
my $tmweights = join($/, @tmweightstrings);

my $latticeparam = ($DOING_LATTICES == 1) 
		? "latticecost ||| 1.0 Opt -Inf +Inf -1 +1"
		: "";
my $latticeweight = ($DOING_LATTICES == 1)
		? "latticecost 1.0"
		: "";

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
			s/<LATTICEWEIGHT>/$latticeweight/g;
			s/<LATTICEPARAM>/$latticeparam/g;
			s/<LMFILE>/$LMFILES[0]/g;
			s/<LMTYPE>/$LM_TYPE/g;
			s/<MEM>/$JOSHUA_MEM/g;
			s/<GRAMMAR_TYPE>/$GRAMMAR_TYPE/g;
			s/<GRAMMAR_FILE>/$TUNE_GRAMMAR/g;
			s/<GLUE_GRAMMAR>/$GLUE_GRAMMAR_FILE/g;
			s/<OOV>/$OOV/g;
			s/<NUMJOBS>/$NUM_JOBS/g;
			s/<NUMTHREADS>/$NUM_THREADS/g;
			s/<QSUB_ARGS>/$QSUB_ARGS/g;
			s/<OUTPUT>/$tunedir\/tune.output.nbest/g;
			s/<REF>/$TUNE{target}/g;
			s/<JOSHUA>/$JOSHUA/g;
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
										"java -d64 -Xmx2g -cp $JOSHUA/bin joshua.zmert.ZMERT -maxMem 4500 $tunedir/mert.config > $tunedir/mert.log 2>&1",
										$TUNE_GRAMMAR,
										"$tunedir/joshua.config.ZMERT.final",
										"$tunedir/decoder_command",
										"$tunedir/mert.config",
										"$tunedir/params.txt");
		system("ln -sf joshua.config.ZMERT.final $tunedir/joshua.config.final");
  } elsif ($TUNER eq "pro") {
		$cachepipe->cmd("pro-$run",
										"java -d64 -Xmx2g -cp $JOSHUA/bin joshua.pro.PRO -maxMem 4500 $tunedir/pro.config > $tunedir/pro.log 2>&1",
										$TUNE_GRAMMAR,
										"$tunedir/joshua.config.PRO.final",
										"$tunedir/decoder_command",
										"$tunedir/pro.config",
										"$tunedir/params.txt");
		system("ln -sf joshua.config.PRO.final $tunedir/joshua.config.final");
  }
}

maybe_quit("TUNE");

# prepare the testing data
if (! $PREPPED{TEST} and $DO_PREPARE_CORPORA) {
  my $prefixes = prepare_data("test",[$TEST]);
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
  
  if ($DO_FILTER_TM) {
		$TEST_GRAMMAR = "$DATA_DIRS{test}/grammar.filtered.gz";

		$cachepipe->cmd("filter-test",
										"$SCRIPTDIR/training/scat $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $THRAX/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $TEST{source} | $SCRIPTDIR/training/remove-unary-abstract.pl | grep -av '|||  |||' | gzip -9n > $TEST_GRAMMAR",
										$GRAMMAR_FILE,
										$TEST{source},
										$TEST_GRAMMAR);
  }
}

# create the glue file
if (! defined $GLUE_GRAMMAR_FILE) {
  $cachepipe->cmd("glue-test",
									"$SCRIPTDIR/training/scat $TEST_GRAMMAR | java -Xmx1g -cp $THRAX/bin/thrax.jar:$JOSHUA/lib/hadoop-core-0.20.203.0.jar:$JOSHUA/lib/commons-logging-1.1.1.jar edu.jhu.thrax.util.CreateGlueGrammar $THRAX_CONF_FILE > $DATA_DIRS{test}/grammar.glue",
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

# decode the test set once for each optimization run
for my $run (1..$OPTIMIZER_RUNS) {
  my $testrun = (defined $NAME) ? "test/$NAME/$run" : "test/$run";
  
  system("mkdir -p $testrun") unless -d $testrun;

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

  # copy the config file over
  my $tunedir = (defined $NAME) ? "tune/$NAME/$run" : "tune/$run";
  $cachepipe->cmd("test-joshua-config-from-tune-$run",
									"cat $tunedir/joshua.config.final | perl -pe 's#tune/#test/#; s/mark_oovs=false/mark_oovs=true/; s/use_sent_specific_tm=.*/use_sent_specific_tm=0/; s/keep_sent_specific_tm=true/keep_sent_specific_tm=false/' > $testrun/joshua.config",
									"$tunedir/joshua.config.final",
									"$testrun/joshua.config");

  $cachepipe->cmd("test-decode-$run",
									"./$testrun/decoder_command",
									"$testrun/decoder_command",
									"$DATA_DIRS{test}/grammar.glue",
									$TEST_GRAMMAR,
									"$testrun/test.output.nbest");

  $cachepipe->cmd("remove-oov-$run",
									"cat $testrun/test.output.nbest | perl -pe 's/_OOV//g' > $testrun/test.output.nbest.noOOV",
									"$testrun/test.output.nbest",
									"$testrun/test.output.nbest.noOOV");

  if ($DO_MBR) {
		my $numlines = `cat $TEST{source} | wc -l`;
		$numlines--;

		$cachepipe->cmd("test-onebest-parmbr-$run", 
										"cat $testrun/test.output.nbest.noOOV | java -Xmx1700m -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.decoder.NbestMinRiskReranker false 1 > $testrun/test.output.1best",
										"$testrun/test.output.nbest.noOOV", 
										"$testrun/test.output.1best");
  } else {
		$cachepipe->cmd("test-extract-onebest-$run",
										"java -Xmx500m -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.util.ExtractTopCand $testrun/test.output.nbest $testrun/test.output.1best",
										"$testrun/test.output.nbest.noOOV", 
										"$testrun/test.output.1best");
  }

  $numrefs = get_numrefs($TEST{target});
  $cachepipe->cmd("test-bleu-$run",
									"java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand $testrun/test.output.1best -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > $testrun/test.output.1best.bleu",
									"$testrun/test.output.1best", 
									"$testrun/test.output.1best.bleu");

  # system("cat $testrun/test.output.1best.bleu");
}

# Now average the runs, report BLEU
my @bleus;
my $numrecs = 0;
my $dir = (defined $NAME) ? "test/$NAME" : "test";
open CMD, "grep ' BLEU = ' $dir/*/*bleu |";
while (<CMD>) {
  my @F = split;
  push(@bleus, $F[-1]);
}
close(CMD);
my $final_bleu = sum(@bleus) / (scalar @bleus);

open BLEU, ">$dir/final-bleu" or die "Can't write to $dir/final-bleu";
printf(BLEU "%s / %d = %.4f\n", join(" + ", @bleus), scalar @bleus, $final_bleu);
close(BLEU);

system("cat $dir/final-bleu");
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
  my $prefixes = prepare_data("test",[$TEST]);
  $TEST{source} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$SOURCE";
  $TEST{target} = "$DATA_DIRS{test}/$prefixes->{lowercased}.$TARGET";
  $PREPPED{TEST} = 1;
}

my $testrun = "test/$NAME";
system("mkdir -p $testrun") unless -d $testrun;

# filter the test grammar
if ($TEST_GRAMMAR_FILE) {
  # if a specific test grammar was specified, use that (no filtering)
  $TEST_GRAMMAR = $TEST_GRAMMAR_FILE;
} else {
  # otherwise, use the main grammar, and filter it if requested
  $TEST_GRAMMAR = $GRAMMAR_FILE;
  
  if ($DO_FILTER_TM) {
		$TEST_GRAMMAR = "$DATA_DIRS{test}/grammar.filtered.gz";

		$cachepipe->cmd("filter-test-$NAME",
										"$CAT $GRAMMAR_FILE | java -Xmx2g -Dfile.encoding=utf8 -cp $THRAX/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $TEST{source} | $SCRIPTDIR/training/remove-unary-abstract.pl | gzip -9n > $TEST_GRAMMAR",
										$GRAMMAR_FILE,
										$TEST{source},
										$TEST_GRAMMAR);
  }
}

# build the glue grammar if needed
if (! defined $GLUE_GRAMMAR_FILE) {
  $cachepipe->cmd("glue-test-$NAME",
									"$CAT $TEST_GRAMMAR | java -Xmx2g -cp $THRAX/bin/thrax.jar:$JOSHUA/lib/hadoop-core-0.20.203.0.jar:$JOSHUA/lib/commons-logging-1.1.1.jar edu.jhu.thrax.util.CreateGlueGrammar $THRAX_CONF_FILE > $DATA_DIRS{test}/grammar.glue",
									$TEST_GRAMMAR,
									"$DATA_DIRS{test}/grammar.glue");
  $GLUE_GRAMMAR_FILE = "$DATA_DIRS{test}/grammar.glue";
}

if ($TUNEFILES{'joshua.config'} eq $JOSHUA_CONFIG_ORIG) {
  print "* FATAL: for direct tests, I need a (tuned) Joshua config file\n";
  exit 1;
}

# this needs to be in a function since it is done all over the place
open FROM, $TUNEFILES{decoder_command} or die "can't find file '$TUNEFILES{decoder_command}'";
open TO, ">$testrun/decoder_command";
print TO "cat $TEST{source} | \$JOSHUA/joshua-decoder -m $JOSHUA_MEM -threads $NUM_THREADS -c $testrun/joshua.config > $testrun/test.output.nbest 2> $testrun/joshua.log\n";
close(TO);
chmod(0755,"$testrun/decoder_command");

# copy over the config file
system("cat $TUNEFILES{'joshua.config'} | $COPY_CONFIG -tm_file $TEST_GRAMMAR -glue_file $GLUE_GRAMMAR_FILE -default_non_terminal $OOV -mark_oovs true > $testrun/joshua.config");

# decode
$cachepipe->cmd("test-$NAME-decode-run",
								"./$testrun/decoder_command",
								"$testrun/decoder_command",
								$TEST_GRAMMAR,
								$GLUE_GRAMMAR_FILE,
								"$testrun/test.output.nbest");

$cachepipe->cmd("test-$NAME-remove-oov",
								"cat $testrun/test.output.nbest | perl -pe 's/_OOV//g' > $testrun/test.output.nbest.noOOV",
								"$testrun/test.output.nbest",
								"$testrun/test.output.nbest.noOOV");

if ($DO_MBR) {
  $cachepipe->cmd("test-$NAME-onebest-parmbr", 
									"cat $testrun/test.output.nbest.noOOV | java -Xmx1700m -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.decoder.NbestMinRiskReranker false 1 > $testrun/test.output.1best",
									"$testrun/test.output.nbest.noOOV", 
									"$testrun/test.output.1best");
} else {
  $cachepipe->cmd("test-$NAME-extract-onebest",
									"java -Xmx500m -cp $JOSHUA/bin -Dfile.encoding=utf8 joshua.util.ExtractTopCand $testrun/test.output.nbest $testrun/test.output.1best",
									"$testrun/test.output.nbest.noOOV", 
									"$testrun/test.output.1best");
}

$numrefs = get_numrefs($TEST{target});
$cachepipe->cmd("$NAME-test-bleu",
								"java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Djava.library.path=lib -Xmx1000m -Xms1000m -Djava.util.logging.config.file=logging.properties joshua.util.JoshuaEval -cand $testrun/test.output.1best -ref $TEST{target} -rps $numrefs -m BLEU 4 closest > $testrun/test.output.1best.bleu",
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
				$cachepipe->cmd("$label-tokenize-$lang",
												"$CAT $DATA_DIRS{$label}/$prefix.$lang.gz | $NORMALIZER $lang | $TOKENIZER -l $lang 2> /dev/null | gzip -9n > $DATA_DIRS{$label}/$prefix.tok.$lang.gz",
												"$DATA_DIRS{$label}/$prefix.$lang.gz", "$DATA_DIRS{$label}/$prefix.tok.$lang.gz");
			}

		}
  }
  # extend the prefix
  $prefix .= ".tok";
  $prefixes{tokenized} = $prefix;

  if ($label eq "train" and $maxlen > 0) {
		# trim training data
		$cachepipe->cmd("train-trim",
										"paste <(gzip -cd $DATA_DIRS{$label}/$prefix.$TARGET.gz) <(gzip -cd $DATA_DIRS{$label}/$prefix.$SOURCE.gz) | $SCRIPTDIR/training/trim_parallel_corpus.pl $maxlen | $SCRIPTDIR/training/split2files.pl $DATA_DIRS{$label}/$prefix.$maxlen.$TARGET.gz $DATA_DIRS{$label}/$prefix.$maxlen.$SOURCE.gz",
										"$DATA_DIRS{$label}/$prefix.$TARGET.gz", 
										"$DATA_DIRS{$label}/$prefix.$SOURCE.gz",
										"$DATA_DIRS{$label}/$prefix.$maxlen.$TARGET.gz", 
										"$DATA_DIRS{$label}/$prefix.$maxlen.$SOURCE.gz",
				);
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
  }
  
  $ENV{HADOOP} = $HADOOP = "hadoop";
}

sub stop_hadoop_cluster {
  if ($HADOOP ne "hadoop") {
		system("hadoop/bin/stop-all.sh");
  }
}

sub teardown_hadoop_cluster {
  stop_hadoop_cluster();
  system("rm -rf hadoop-0.20.203.0 hadoop");
}

sub is_lattice {
  my $file = shift;
  open READ, "$CAT $file|" or die "can't read from potential lattice '$file'";
  my $line = <READ>;
  close(READ);
  if ($line =~ /^\(\(\(/) {
		$DOING_LATTICES = 1;
		return 1;
  } else {
		return 0;
  }
}

# This function runs GIZA++, possibly doing both directions at the same time
sub run_giza {
  my ($chunkdir,$chunkno,$do_parallel) = @_;
  my $parallel = ($do_parallel == 1) ? "-parallel" : "";
  $cachepipe->cmd("giza-$chunkno",
									"rm -f $chunkdir/corpus.0-0.*; $GIZA_TRAINER --root-dir $chunkdir -e $TARGET.$chunkno -f $SOURCE.$chunkno -corpus $DATA_DIRS{train}/splits/corpus -merge $GIZA_MERGE $parallel > $chunkdir/giza.log 2>&1",
									"$DATA_DIRS{train}/splits/corpus.$SOURCE.$chunkno",
									"$DATA_DIRS{train}/splits/corpus.$TARGET.$chunkno",
									"$chunkdir/model/aligned.grow-diag-final");
}

sub run_berkeley_aligner {
  my ($chunkdir,$chunkno) = @_;

  # copy and modify the config file
  open FROM, "$JOSHUA/scripts/training/templates/alignment/word-align.conf" or die "can't read berkeley alignment template";
  open TO, ">", "alignments/$chunkno/word-align.conf" or die "can't write to 'alignments/$chunkno/word-align.conf'";
  while (<FROM>) {
		s/<SOURCE>/$SOURCE.$chunkno/g;
		s/<TARGET>/$TARGET.$chunkno/g;
		s/<CHUNK>/$chunkno/g;
		s/<TRAIN_DIR>/$DATA_DIRS{train}/g;
		print TO;
  }
  close(TO);
  close(FROM);

  # run the job
  $cachepipe->cmd("berkeley-aligner-chunk-$chunkno",
									"java -d64 -Xmx${ALIGNER_MEM} -jar $JOSHUA/lib/berkeleyaligner.jar ++alignments/$chunkno/word-align.conf",
									"alignments/$chunkno/word-align.conf",
									"$DATA_DIRS{train}/splits/corpus.$SOURCE.$chunkno",
									"$DATA_DIRS{train}/splits/corpus.$TARGET.$chunkno",
									"$chunkdir/training.align");
}

# This counts the number of TM features present in a grammar
sub count_num_features {
  my ($grammar) = @_;

  open GRAMMAR, "$CAT $grammar|" or die "FATAL: can't read $grammar";
  chomp(my $line = <GRAMMAR>);
  close(GRAMMAR);

  my @tokens = split(/ \|\|\| /, $line);
  my @numfeatures = split(' ', $tokens[-1]);
	my $num = scalar(@numfeatures);

  return scalar @numfeatures;
}

# File names reflecting relative paths need to be absolute-ized for --rundir to work
sub get_absolute_path {
	my ($file) = @_;

	if (defined $file) {
		# prepend startdir (which is absolute) unless the path is absolute.
		$file = "$STARTDIR/$file" unless $file =~ /^\//;
	}

	return $file;
}
