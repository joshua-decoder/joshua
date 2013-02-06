#!/usr/bin/perl -w
#
# Usage:
# mert-moses.pl <foreign> <english> <decoder-executable> <decoder-config>
# For other options see below or run 'mert-moses.pl --help'

# Notes:
# <foreign> and <english> should be raw text files, one sentence per line
# <english> can be a prefix, in which case the files are <english>0, <english>1, etc. are used

# Borrowed from mert-moses.pl, originally by Philipp Koehn

use strict;
use warnings;
use FindBin qw($RealBin);
use File::Basename;
use File::Path;
use File::Spec;
use Cwd;

my $JOSHUA = $ENV{JOSHUA};

my $SCRIPTS_ROOTDIR = $RealBin;
$SCRIPTS_ROOTDIR =~ s/\/training$//;
$SCRIPTS_ROOTDIR = $ENV{"SCRIPTS_ROOTDIR"} if defined($ENV{"SCRIPTS_ROOTDIR"});

## We preserve this bit of comments to keep the traditional weight ranges.
#     "w" => [ [ 0.0, -1.0, 1.0 ] ],  # word penalty
#     "d"  => [ [ 1.0, 0.0, 2.0 ] ],  # lexicalized reordering model
#     "lm" => [ [ 1.0, 0.0, 2.0 ] ],  # language model
#     "g"  => [ [ 1.0, 0.0, 2.0 ],    # generation model
# 	      [ 1.0, 0.0, 2.0 ] ],
#     "tm" => [ [ 0.3, 0.0, 0.5 ],    # translation model
# 	      [ 0.2, 0.0, 0.5 ],
# 	      [ 0.3, 0.0, 0.5 ],
# 	      [ 0.2, 0.0, 0.5 ],
# 	      [ 0.0,-1.0, 1.0 ] ],  # ... last weight is phrase penalty
#     "lex"=> [ [ 0.1, 0.0, 0.2 ] ],  # global lexical model
#     "I"  => [ [ 0.0,-1.0, 1.0 ] ],  # input lattice scores



# moses.ini file uses FULL names for lambdas, while this training script
# internally (and on the command line) uses ABBR names.
my @ABBR_FULL_MAP = qw(d=weight-d lm=weight-l tm=weight-t w=weight-w
  g=weight-generation lex=weight-lex I=weight-i dlm=weight-dlm pp=weight-pp wt=weight-wt pb=weight-pb lex=weight-lex glm=weight-glm);
my %ABBR2FULL = map { split /=/, $_, 2 } @ABBR_FULL_MAP;
my %FULL2ABBR = map { my ($a, $b) = split /=/, $_, 2; ($b, $a); } @ABBR_FULL_MAP;

my $minimum_required_change_in_weights = 0.00001;
    # stop if no lambda changes more than this

my $verbose = 0;
my $usage = 0; # request for --help

# We assume that if you don't specify working directory,
# we set the default is set to `pwd`/mert-work
my $___WORKING_DIR = File::Spec->catfile(Cwd::getcwd(), "mert-work");
my $___DEV_F = undef; # required, input text to decode
my $___DEV_E = undef; # required, basename of files with references
my $___DECODER = undef; # required, pathname to the decoder executable
my $___CONFIG = undef; # required, pathname to startup ini file
my $___N_BEST_LIST_SIZE = 100;
my $___LATTICE_SAMPLES = 0;
my $queue_flags = "-hard";  # extra parameters for parallelizer
      # the -l ws0ssmt was relevant only to JHU 2006 workshop
my $___JOBS = undef; # if parallel, number of jobs to use (undef or 0 -> serial)
my $___DECODER_FLAGS = ""; # additional parametrs to pass to the decoder
my $continue = 0; # should we try to continue from the last saved step?
my $skip_decoder = 0; # and should we skip the first decoder run (assuming we got interrupted during mert)
my $___PREDICTABLE_SEEDS = 0;
my $___START_WITH_HISTORIC_BESTS = 0; # use best settings from all previous iterations as starting points [Foster&Kuhn,2009]
my $___RANDOM_DIRECTIONS = 0; # search in random directions only
my $___NUM_RANDOM_DIRECTIONS = 0; # number of random directions, also works with default optimizer [Cer&al.,2008]
my $___RANDOM_RESTARTS = 20;
my $___RETURN_BEST_DEV = 1; # return the best weights according to dev, not the last

# Flags related to PRO (Hopkins & May, 2011)
my $___PAIRWISE_RANKED_OPTIMIZER = 0; # flag to enable PRO.
my $___PRO_STARTING_POINT = 0; # get a starting point from pairwise ranked optimizer
my $___HISTORIC_INTERPOLATION = 0; # interpolate optimize weights with previous iteration's weights [Hopkins&May,2011,5.4.3]
# MegaM's options for PRO optimization.
# TODO: Should we also add these values to options of this script?
my $megam_default_options = "-fvals -maxi 30 -nobias binary";

# Flags related to Batch MIRA (Cherry & Foster, 2012)
my $___BATCH_MIRA = 0; # flg to enable batch MIRA

my $__THREADS = 0;

# Parameter for effective reference length when computing BLEU score
# Default is to use shortest reference
# Use "--shortest" to use shortest reference length
# Use "--average" to use average reference length
# Use "--closest" to use closest reference length
# Only one between --shortest, --average and --closest can be set
# If more than one choice the defualt (--shortest) is used
my $___SHORTEST = 0;
my $___AVERAGE = 0;
my $___CLOSEST = 0;

# Use "--nocase" to compute case-insensitive scores
my $___NOCASE = 0;

# Use "--nonorm" to non normalize translation before computing scores
my $___NONORM = 0;

# set 0 if input type is text, set 1 if input type is confusion network
my $___INPUTTYPE = 0;


my $mertdir = undef; # path to new mert directory
my $mertargs = undef; # args to pass through to mert & extractor
my $mertmertargs = undef; # args to pass through to mert only
my $extractorargs = undef; # args to pass through to extractor only

# Args to pass through to batch mira only.  This flags is useful to
# change MIRA's hyperparameters such as regularization parameter C,
# BLEU decay factor, and the number of iterations of MIRA.
my $batch_mira_args = undef;

my $filtercmd = undef; # path to filter-model-given-input.pl
my $filterfile = undef;
my $qsubwrapper = undef;
my $moses_parallel_cmd = undef;
my $old_sge = 0; # assume sge<6.0
my $___CONFIG_ORIG = undef; # pathname to startup ini file before filtering
my $___ACTIVATE_FEATURES = undef; # comma-separated (or blank-separated) list of features to work on
                                  # if undef work on all features
                                  # (others are fixed to the starting values)
my $___RANGES = undef;
my $___USE_CONFIG_WEIGHTS_FIRST = 0; # use weights in configuration file for first iteration
my $prev_aggregate_nbl_size = -1; # number of previous step to consider when loading data (default =-1)
                                  # -1 means all previous, i.e. from iteration 1
                                  # 0 means no previous data, i.e. from actual iteration
                                  # 1 means 1 previous data , i.e. from the actual iteration and from the previous one
                                  # and so on
my $maximum_iterations = 15;

use Getopt::Long;
GetOptions(
  "working-dir=s" => \$___WORKING_DIR,
  "input=s" => \$___DEV_F,
  "inputtype=i" => \$___INPUTTYPE,
  "refs=s" => \$___DEV_E,
  "decoder=s" => \$___DECODER,
  "config=s" => \$___CONFIG,
  "nbest=i" => \$___N_BEST_LIST_SIZE,
  "lattice-samples=i" => \$___LATTICE_SAMPLES,
  "queue-flags=s" => \$queue_flags,
  "jobs=i" => \$___JOBS,
  "decoder-flags=s" => \$___DECODER_FLAGS,
  "continue" => \$continue,
  "skip-decoder" => \$skip_decoder,
  "shortest" => \$___SHORTEST,
  "average" => \$___AVERAGE,
  "closest" => \$___CLOSEST,
  "nocase" => \$___NOCASE,
  "nonorm" => \$___NONORM,
  "help" => \$usage,
  "verbose" => \$verbose,
  "mertdir=s" => \$mertdir,
  "mertargs=s" => \$mertargs,
  "extractorargs=s" => \$extractorargs,
  "mertmertargs=s" => \$mertmertargs,
  "rootdir=s" => \$SCRIPTS_ROOTDIR,
  "filtercmd=s" => \$filtercmd, # allow to override the default location
  "filterfile=s" => \$filterfile, # input to filtering script (useful for lattices/confnets)
  "qsubwrapper=s" => \$qsubwrapper, # allow to override the default location
  "mosesparallelcmd=s" => \$moses_parallel_cmd, # allow to override the default location
  "old-sge" => \$old_sge, #passed to moses-parallel
  "predictable-seeds" => \$___PREDICTABLE_SEEDS, # make random restarts deterministic
  "historic-bests" => \$___START_WITH_HISTORIC_BESTS, # use best settings from all previous iterations as starting points
  "random-directions" => \$___RANDOM_DIRECTIONS, # search only in random directions
  "number-of-random-directions=i" => \$___NUM_RANDOM_DIRECTIONS, # number of random directions
  "random-restarts=i" => \$___RANDOM_RESTARTS, # number of random restarts
  "return-best-dev" => \$___RETURN_BEST_DEV, # return the best weights according to dev, not the last
  "activate-features=s" => \$___ACTIVATE_FEATURES, #comma-separated (or blank-separated) list of features to work on (others are fixed to the starting values)
  "range=s@" => \$___RANGES,
  "use-config-weights-for-first-run" => \$___USE_CONFIG_WEIGHTS_FIRST, # use the weights in the configuration file when running the decoder for the first time
  "prev-aggregate-nbestlist=i" => \$prev_aggregate_nbl_size, #number of previous step to consider when loading data (default =-1, i.e. all previous)
  "maximum-iterations=i" => \$maximum_iterations,
  "pairwise-ranked" => \$___PAIRWISE_RANKED_OPTIMIZER,
  "pro-starting-point" => \$___PRO_STARTING_POINT,
  "historic-interpolation=f" => \$___HISTORIC_INTERPOLATION,
  "batch-mira" => \$___BATCH_MIRA,
  "batch-mira-args=s" => \$batch_mira_args,
  "threads=i" => \$__THREADS
) or exit(1);

# the 4 required parameters can be supplied on the command line directly
# or using the --options
if (scalar @ARGV == 4) {
  # required parameters: input_file references_basename decoder_executable
  $___DEV_F = shift;
  $___DEV_E = shift;
  $___DECODER = shift;
  $___CONFIG = shift;
}

if ($usage || !defined $___DEV_F || !defined $___DEV_E || !defined $___DECODER || !defined $___CONFIG) {
  print STDERR "usage: $0 input-text references decoder-executable decoder.ini
Options:
  --working-dir=mert-dir ... where all the files are created
  --nbest=100            ... how big nbestlist to generate
  --lattice-samples      ... how many lattice samples (Chatterjee & Cancedda, emnlp 2010)
  --jobs=N               ... set this to anything to run moses in parallel
  --mosesparallelcmd=STR ... use a different script instead of moses-parallel
  --queue-flags=STRING   ... anything you with to pass to qsub, eg.
                             '-l ws06osssmt=true'. The default is: '-hard'
                             To reset the parameters, please use
                             --queue-flags=' '
                             (i.e. a space between the quotes).
  --decoder-flags=STRING ... extra parameters for the decoder
  --continue             ... continue from the last successful iteration
  --skip-decoder         ... skip the decoder run for the first time,
                             assuming that we got interrupted during
                             optimization
  --shortest --average --closest
                         ... Use shortest/average/closest reference length
                             as effective reference length (mutually exclusive)
  --nocase               ... Do not preserve case information; i.e.
                             case-insensitive evaluation (default is false).
  --nonorm               ... Do not use text normalization (flag is not active,
                             i.e. text is NOT normalized)
  --filtercmd=STRING     ... path to filter-model-given-input.pl
  --filterfile=STRING    ... path to alternative to input-text for filtering
                             model. useful for lattice decoding
  --rootdir=STRING       ... where do helpers reside (if not given explicitly)
  --mertdir=STRING       ... path to new mert implementation
  --mertargs=STRING      ... extra args for both extractor and mert
  --extractorargs=STRING ... extra args for extractor only
  --mertmertargs=STRING  ... extra args for mert only
  --scorenbestcmd=STRING ... path to score-nbest.py
  --old-sge              ... passed to parallelizers, assume Grid Engine < 6.0
  --inputtype=[0|1|2]    ... Handle different input types: (0 for text,
                             1 for confusion network, 2 for lattices,
                             default is 0)
  --no-filter-phrase-table ... disallow filtering of phrase tables
                              (useful if binary phrase tables are available)
  --random-restarts=INT  ... number of random restarts (default: 20)
  --predictable-seeds    ... provide predictable seeds to mert so that random
                             restarts are the same on every run
  --range=tm:0..1,-1..1  ... specify min and max value for some features
                             --range can be repeated as needed.
                             The order of the various --range specifications
                             is important only within a feature name.
                             E.g.:
                               --range=tm:0..1,-1..1 --range=tm:0..2
                             is identical to:
                               --range=tm:0..1,-1..1,0..2
                             but not to:
                               --range=tm:0..2 --range=tm:0..1,-1..1
  --activate-features=STRING  ... comma-separated list of features to optimize,
                                  others are fixed to the starting values
                                  default: optimize all features
                                  example: tm_0,tm_4,d_0
  --prev-aggregate-nbestlist=INT ... number of previous step to consider when
                                     loading data (default = $prev_aggregate_nbl_size)
                                    -1 means all previous, i.e. from iteration 1
                                     0 means no previous data, i.e. only the
                                       current iteration
                                     N means this and N previous iterations

  --maximum-iterations=ITERS ... Maximum number of iterations. Default: $maximum_iterations
  --return-best-dev          ... Return the weights according to dev bleu, instead of returning
                                 the last iteration
  --random-directions               ... search only in random directions
  --number-of-random-directions=int ... number of random directions
                                        (also works with regular optimizer, default: 0)
  --pairwise-ranked         ... Use PRO for optimisation (Hopkins and May, emnlp 2011)
  --pro-starting-point      ... Use PRO to get a starting point for MERT
  --batch-mira              ... Use Batch MIRA for optimisation (Cherry and Foster, NAACL 2012)
  --batch-mira-args=STRING  ... args to pass through to batch MIRA. This flag is useful to
                                change MIRA's hyperparameters such as regularization parameter C,
                                BLEU decay factor, and the number of iterations of MIRA.
  --threads=NUMBER          ... Use multi-threaded mert (must be compiled in).
  --historic-interpolation  ... Interpolate optimized weights with prior iterations' weight
                                (parameter sets factor [0;1] given to current weights)
";
  exit 1;
}


# Check validity of input parameters and set defaults if needed

print STDERR "Using SCRIPTS_ROOTDIR: $SCRIPTS_ROOTDIR\n";

# path of script for filtering phrase tables and running the decoder
$filtercmd = File::Spec->catfile($SCRIPTS_ROOTDIR, "training", "filter-model-given-input.pl") if !defined $filtercmd;

$qsubwrapper = File::Spec->catfile($SCRIPTS_ROOTDIR, "generic", "qsub-wrapper.pl") if !defined $qsubwrapper;

$moses_parallel_cmd = File::Spec->catfile($SCRIPTS_ROOTDIR, "generic", "moses-parallel.pl")
  if !defined $moses_parallel_cmd;

if (!defined $mertdir) {
  $mertdir = File::Spec->catfile(File::Basename::dirname($SCRIPTS_ROOTDIR), "bin");
  die "mertdir does not exist: $mertdir" if ! -x $mertdir;
  print STDERR "Assuming --mertdir=$mertdir\n";
}

my $mert_extract_cmd = File::Spec->catfile($mertdir, "extractor");
my $mert_mert_cmd    = File::Spec->catfile($mertdir, "mert");
my $mert_pro_cmd     = File::Spec->catfile($mertdir, "pro");
my $mert_mira_cmd    = File::Spec->catfile($mertdir, "kbmira");
my $mert_eval_cmd    = File::Spec->catfile($mertdir, "evaluator");

die "Not executable: $mert_extract_cmd" if ! -x $mert_extract_cmd;
die "Not executable: $mert_mert_cmd"    if ! -x $mert_mert_cmd;
die "Not executable: $mert_pro_cmd"     if ! -x $mert_pro_cmd;
die "Not executable: $mert_mira_cmd"    if ! -x $mert_mira_cmd;
die "Not executable: $mert_eval_cmd"    if ! -x $mert_eval_cmd;

my $pro_optimizer = File::Spec->catfile($mertdir, "megam_i686.opt");  # or set to your installation

if (($___PAIRWISE_RANKED_OPTIMIZER || $___PRO_STARTING_POINT) && ! -x $pro_optimizer) {
  print "Could not find $pro_optimizer, installing it in $mertdir\n";
  my $megam_url = "http://www.cs.utah.edu/~hal/megam/";
  if (&is_mac_osx()) {
    die "Error: Sorry for Mac OS X users! Please get the source code of megam and compile by hand. Please see $megam_url for details.";
  }

  `cd $mertdir; wget http://www.cs.utah.edu/~hal/megam/megam_i686.opt.gz;`;
  `gunzip $pro_optimizer.gz`;
  `chmod +x $pro_optimizer`;
  die("ERROR: Installation of megam_i686.opt failed! Install by hand from $megam_url") unless -x $pro_optimizer;
}

$mertargs = "" if !defined $mertargs;

my $scconfig = undef;
if ($mertargs =~ /\-\-scconfig\s+(.+?)(\s|$)/) {
  $scconfig = $1;
  $scconfig =~ s/\,/ /g;
  $mertargs =~ s/\-\-scconfig\s+(.+?)(\s|$)//;
}

# handling reference lengh strategy
$scconfig .= &setup_reference_length_type();

# handling case-insensitive flag
$scconfig .= &setup_case_config();

$scconfig =~ s/^\s+//;
$scconfig =~ s/\s+$//;
$scconfig =~ s/\s+/,/g;

$scconfig = "--scconfig $scconfig" if ($scconfig);

my $mert_extract_args = $mertargs;
$mert_extract_args .= " $scconfig";

$extractorargs = "" unless $extractorargs;
$mert_extract_args .= " $extractorargs";

$mertmertargs = "" if !defined $mertmertargs;

my $mert_mert_args = "$mertargs $mertmertargs";
$mert_mert_args =~ s/\-+(binary|b)\b//;
$mert_mert_args .= " $scconfig";
if ($___ACTIVATE_FEATURES) {
  $mert_mert_args .= " -o \"$___ACTIVATE_FEATURES\"";
}

my ($just_cmd_filtercmd, $x) = split(/ /, $filtercmd);
die "Not executable: $just_cmd_filtercmd" if ! -x $just_cmd_filtercmd;
die "Not executable: $moses_parallel_cmd" if defined $___JOBS && ! -x $moses_parallel_cmd;
die "Not executable: $qsubwrapper"        if defined $___JOBS && ! -x $qsubwrapper;
die "Not executable: $___DECODER"         if ! -x $___DECODER;

my $input_abs = ensure_full_path($___DEV_F);
die "File not found: $___DEV_F (interpreted as $input_abs)." if ! -e $input_abs;

$___DEV_F = $input_abs;

# Option to pass to qsubwrapper and moses-parallel
my $pass_old_sge = $old_sge ? "-old-sge" : "";

my $decoder_abs = ensure_full_path($___DECODER);
die "File not executable: $___DECODER (interpreted as $decoder_abs)."
  if ! -x $decoder_abs;
$___DECODER = $decoder_abs;

my $ref_abs = ensure_full_path($___DEV_E);
# check if English dev set (reference translations) exist and store a list of all references
my @references;
if (-e $ref_abs) {
  push @references, $ref_abs;
} else {
  # if multiple file, get a full list of the files
  my $part = 0;
  if (! -e $ref_abs . "0" && -e $ref_abs . ".ref0") {
    $ref_abs .= ".ref";
  }
  while (-e $ref_abs . $part) {
    push @references, $ref_abs . $part;
    $part++;
  }
  die("Reference translations not found: $___DEV_E (interpreted as $ref_abs)") unless $part;
}

my $config_abs = ensure_full_path($___CONFIG);
die "File not found: $___CONFIG (interpreted as $config_abs)." if ! -e $config_abs;
$___CONFIG = $config_abs;

# moses should use our config
if ($___DECODER_FLAGS =~ /(^|\s)-(config|f) /
    || $___DECODER_FLAGS =~ /(^|\s)-(ttable-file|t) /
    || $___DECODER_FLAGS =~ /(^|\s)-(distortion-file) /
    || $___DECODER_FLAGS =~ /(^|\s)-(generation-file) /
    || $___DECODER_FLAGS =~ /(^|\s)-(lmodel-file) /
    || $___DECODER_FLAGS =~ /(^|\s)-(global-lexical-file) /
  ) {
  die "It is forbidden to supply any of -config, -ttable-file, -distortion-file, -generation-file or -lmodel-file in the --decoder-flags.\nPlease use only the --config option to give the config file that lists all the supplementary files.";
}

# as weights are normalized in the next steps (by cmert)
# normalize initial LAMBDAs, too
my $need_to_normalize = 1;

#store current directory and create the working directory (if needed)
my $cwd = Cwd::getcwd();

$___WORKING_DIR = ensure_full_path($___WORKING_DIR);

mkpath($___WORKING_DIR);

# open local scope
{

#chdir to the working directory
chdir($___WORKING_DIR) or die "Can't chdir to $___WORKING_DIR";

# fixed file names
my $mert_outfile = "mert.out";
my $mert_logfile = "mert.log";
my $finished_step_file = "finished_step.txt";

# set start run
my $start_run = 1;
my $bestpoint = undef;
my $devbleu = undef;
my $weights_file = undef;

my $prev_feature_file = undef;
my $prev_score_file = undef;
my $prev_init_file = undef;

# do not filter phrase tables (useful if binary phrase tables are available)
# use the original configuration file
$___CONFIG_ORIG = $___CONFIG;

# Read the list of weights from the weights file
chomp($weights_file = `cat $___CONFIG | grep ^weights-file | awk '{print \$NF}'`);
my $featlist = get_featlist_from_file($weights_file);

$featlist = insert_ranges_to_featlist($featlist, $___RANGES);

# Mark which features are disabled:
# all enabled
foreach my $name (keys %$featlist) {
  $featlist->{$name}{enabled} = 1;
}

print STDERR "MERT starting values and ranges for random generation:\n";
foreach my $name (keys %$featlist) {
  my $val = $featlist->{$name}{value};
  my $min = $featlist->{$name}{min};
  my $max = $featlist->{$name}{max};
  my $enabled = $featlist->{$name}{enabled};
  printf STDERR "  %5s = %7.3f", $name, $val;
  if ($enabled) {
    printf STDERR " (%5.2f .. %5.2f)\n", $min, $max;
  } else {
    print STDERR " --- inactive, not optimized ---\n";
  }
}

if ($continue) {
  # getting the last finished step
  print STDERR "Trying to continue an interrupted optimization.\n";
  open my $fh, '<', $finished_step_file or die "$finished_step_file: $!";
  my $step = <$fh>;
  chomp $step;
  close $fh;

  print STDERR "Last finished step is $step\n";

  # getting the first needed step
  my $firststep;
  if ($prev_aggregate_nbl_size == -1) {
    $firststep = 1;
  } else {
    $firststep = $step - $prev_aggregate_nbl_size + 1;
    $firststep = ($firststep > 0) ? $firststep : 1;
  }

  #checking if all needed data are available
  if ($firststep <= $step) {
    print STDERR "First previous needed data index is $firststep\n";
    print STDERR "Checking whether all needed data (from step $firststep to step $step) are available\n";

    for (my $prevstep = $firststep; $prevstep <= $step; $prevstep++) {
        print STDERR "Checking whether data of step $prevstep are available\n";
      if (! -e "run$prevstep.features.dat") {
          die "Can't start from step $step, because run$prevstep.features.dat was not found!";
      } else {
        if (defined $prev_feature_file) {
          $prev_feature_file = "${prev_feature_file},run$prevstep.features.dat";
        } else {
          $prev_feature_file = "run$prevstep.features.dat";
        }
      }
      if (! -e "run$prevstep.scores.dat") {
          die "Can't start from step $step, because run$prevstep.scores.dat was not found!";
      } else {
        if (defined $prev_score_file) {
          $prev_score_file = "${prev_score_file},run$prevstep.scores.dat";
        } else {
          $prev_score_file = "run$prevstep.scores.dat";
        }
      }
    }
    if (! -e "run$step.weights.txt") {
      die "Can't start from step $step, because run$step.weights.txt was not found!";
    }
    if (! -e "run$step.$mert_logfile") {
      die "Can't start from step $step, because run$step.$mert_logfile was not found!";
    }
    if (! -e "run$step.best$___N_BEST_LIST_SIZE.out.gz") {
      die "Can't start from step $step, because run$step.best$___N_BEST_LIST_SIZE.out.gz was not found!";
    }
    print STDERR "All needed data are available\n";
    print STDERR "Loading information from last step ($step)\n";

    ($bestpoint, $devbleu) = &get_weights_from_mert("run$step.$mert_outfile","run$step.$mert_logfile", scalar(keys(%$featlist)));
    die "Failed to parse mert.log, missed Best point there."
      if !defined $bestpoint || !defined $devbleu;
    print "($step) BEST at $step $bestpoint => $devbleu at ".`date`;
    my %newweights = %$bestpoint;

    # update my cache of lambda values
    map { $featlist->{$_}{value} = $newweights{$_}{value} } (keys %newweights);
  } else {
    print STDERR "No previous data are needed\n";
  }
  $start_run = $step + 1;
} # end continue loop

###### MERT MAIN LOOP

my $run = $start_run - 1;

my $oldallsorted    = undef;
my $allsorted       = undef;
my $nbest_file      = undef;
my $lsamp_file      = undef; # Lattice samples
my $orig_nbest_file = undef; # replaced if lattice sampling

while (1) {
  $run++;
  if ($maximum_iterations && $run > $maximum_iterations) {
    print "Maximum number of iterations exceeded - stopping\n";
    last;
  }
  # run beamdecoder with option to output nbestlists
  # the end result should be (1) @NBEST_LIST, a list of lists; (2) @SCORE, a list of lists of lists

  print "run $run start at ".`date`;

  # In case something dies later, we might wish to have a copy
  create_config($weights_file, "./run$run.weights", $featlist, $run, (defined $devbleu ? $devbleu : "--not-estimated--"));
  system("cp run$run.weights $weights_file");

  # skip running the decoder if the user wanted
  if (! $skip_decoder) {
    print "($run) run decoder to produce n-best lists\n";
    ($nbest_file, $lsamp_file) = run_decoder($featlist, $run, $need_to_normalize);
    $need_to_normalize = 0;
    if ($___LATTICE_SAMPLES) {
      my $combined_file = "$nbest_file.comb";
      safesystem("sort -k1,1n $nbest_file $lsamp_file > $combined_file") or
          die("failed to merge nbest and lattice samples");
      safesystem("gzip -f $nbest_file; gzip -f $lsamp_file") or
          die "Failed to gzip nbests and lattice samples";
      $orig_nbest_file = "$nbest_file.gz";
      $orig_nbest_file = "$nbest_file.gz";
      $lsamp_file      = "$lsamp_file.gz";
      $lsamp_file      = "$lsamp_file.gz";
      $nbest_file      = "$combined_file";
    }
    safesystem("gzip -f $nbest_file") or die "Failed to gzip run*out";
    $nbest_file = $nbest_file.".gz";
  } else {
    $nbest_file = "run$run.best$___N_BEST_LIST_SIZE.out.gz";
    print "skipped decoder run $run\n";
    $skip_decoder = 0;
    $need_to_normalize = 0;
  }

  # extract score statistics and features from the nbest lists
  print STDERR "Scoring the nbestlist.\n";

  my $base_feature_file = "features.dat";
  my $base_score_file   = "scores.dat";
  my $feature_file      = "run$run.${base_feature_file}";
  my $score_file        = "run$run.${base_score_file}";

  my $cmd = "$mert_extract_cmd $mert_extract_args --scfile $score_file --ffile $feature_file -r " . join(",", @references) . " -n $nbest_file";

  $cmd = &create_extractor_script($cmd, $___WORKING_DIR);
  &submit_or_exec($cmd, "extract.out","extract.err");

  # Remove the trailing underscores from feature labels, which were introduced to trick the Moses'
  # extractor into treating everything as a sparse feature.
  system("perl -pi -e 's/(\\S+)_:(\\S+)/\$1:\$2/g' $feature_file");

  # We also need to rename features that contain a colon in them, since that causes MIRA to
  # barf. This could be addressed by having MIRA split the feature name on the *last* colon it
  # finds, but its easier to use Moses internal tools unmodified. This is mostly for fragmentLM
  # features. Colons appear in preterminal names (both left side :_ and right side _:) and in
  # terminals (":").
  system("perl -pi -e 's/:_/-COLON-_/g; s/_:/_-COLON-/g; s/\":\"/\"-COLON-\"/g' $feature_file");

  my %CURR;
  map { $CURR{$_} = $featlist->{$_}{value} } keys(%$featlist); # save the current features
  my $DIM = scalar(keys(%CURR)); # number of lambdas
  # print "\n**CURR: " . join(" ", map { "$_:$CURR{$_}" } (keys(%CURR))) . $/;

  # run mert
  $cmd = "$mert_mert_cmd -d $DIM $mert_mert_args";

  my $mert_settings = " -n $___RANDOM_RESTARTS";
  my $seed_settings = "";
  if ($___PREDICTABLE_SEEDS) {
    my $seed = $run * 1000;
    $seed_settings .= " -r $seed";
  }
  $mert_settings .= $seed_settings;
  if ($___RANDOM_DIRECTIONS) {
    if ($___NUM_RANDOM_DIRECTIONS == 0) {
      $mert_settings .= " -m 50";
    }
    $mert_settings .= " -t random-direction";
  }
  if ($___NUM_RANDOM_DIRECTIONS) {
    $mert_settings .= " -m $___NUM_RANDOM_DIRECTIONS";
  }
  if ($__THREADS) {
    $mert_settings .= " --threads $__THREADS";
  }

  my $ffiles = "";
  my $scfiles = "";

  if (defined $prev_feature_file) {
    $ffiles = "$prev_feature_file,$feature_file";
  } else{
    $ffiles = "$feature_file";
  }

  if (defined $prev_score_file) {
    $scfiles = "$prev_score_file,$score_file";
  } else{
    $scfiles = "$score_file";
  }

  my $mira_settings = "";
  if ($___BATCH_MIRA && $batch_mira_args) {
    $mira_settings .= "$batch_mira_args ";
  }

  my $file_settings = " --ffile $ffiles --scfile $scfiles";
  my $pro_file_settings = "--ffile " . join(" --ffile ", split(/,/, $ffiles)) .
                          " --scfile " .  join(" --scfile ", split(/,/, $scfiles));

  $cmd .= $file_settings;

  my $pro_optimizer_cmd = "$pro_optimizer $megam_default_options run$run.pro.data";
  if ($___PAIRWISE_RANKED_OPTIMIZER) {  # pro optimization
    $cmd = "$mert_pro_cmd $seed_settings $pro_file_settings -o run$run.pro.data ; $pro_optimizer_cmd";
    &submit_or_exec($cmd, $mert_outfile, $mert_logfile);
  } elsif ($___PRO_STARTING_POINT) {  # First, run pro, then mert
    # run pro...
    my $pro_cmd = "$mert_pro_cmd $seed_settings $pro_file_settings -o run$run.pro.data ; $pro_optimizer_cmd";
    &submit_or_exec($pro_cmd, "run$run.pro.out", "run$run.pro.err");
    # ... get results ...
    ($bestpoint,$devbleu) = &get_weights_from_mert("run$run.pro.out","run$run.pro.err",scalar(keys(%$featlist)));
    # Get the pro outputs ready for mert. Add the weight ranges,
    # and a weight and range for the single sparse feature
    $cmd =~ s/--ifile (\S+)/--ifile run$run.init.pro/;
    open(MERT_START,$1);
    open(PRO_START,">run$run.init.pro");
    print PRO_START $bestpoint." 1\n";
    my $mert_line = <MERT_START>;
    $mert_line = <MERT_START>;
    chomp $mert_line;
    print PRO_START $mert_line." 0\n";
    $mert_line = <MERT_START>;
    chomp $mert_line;
    print PRO_START $mert_line." 1\n";
    close(PRO_START);

    # ... and run mert
    $cmd =~ s/(--ifile \S+)/$1,run$run.init.pro/;
    &submit_or_exec($cmd . $mert_settings, $mert_outfile, $mert_logfile);
  } elsif ($___BATCH_MIRA) { # batch MIRA optimization
    $cmd = "$mert_mira_cmd $mira_settings $seed_settings $pro_file_settings -o $mert_outfile";
    &submit_or_exec($cmd, "run$run.mert.out", $mert_logfile);

  } else {  # just mert
    &submit_or_exec($cmd . $mert_settings, $mert_outfile, $mert_logfile);
  }

  # Now replace the -COLON-s that were introduced to prevent confusion from MIRA.
  system("perl -pi -e 's/-COLON-/:/g' $mert_outfile");

  # backup copies
  safesystem("\\cp -f extract.err run$run.extract.err") or die;
  safesystem("\\cp -f extract.out run$run.extract.out") or die;
  safesystem("\\cp -f $mert_outfile run$run.$mert_outfile") or die;
  safesystem("\\cp -f $mert_logfile run$run.$mert_logfile") or die;
  safesystem("touch $mert_logfile run$run.$mert_logfile") or die;

  print "run $run end at ".`date`;

  ($bestpoint,$devbleu) = &get_weights_from_mert("run$run.$mert_outfile","run$run.$mert_logfile",scalar(keys(%$featlist)));

  my %newweights = %$bestpoint;

  die "Failed to parse mert.log, missed Best point there."
    if !defined $bestpoint || !defined $devbleu;

  print "($run) BEST at $run: " . (join(" ", map { "$_:$bestpoint->{$_}" } keys %$bestpoint)) . " => $devbleu at ".`date`;

  foreach my $name (keys(%newweights)) {
    if (exists $featlist->{$name}) {
      $featlist->{$name}{value} = $newweights{$name};
    } else {
      print STDERR "WARNING: no old weight value for updated weight $name\n";
    }
  }

  ## additional stopping criterion: weights have not changed
  my $shouldstop = 1;
  foreach my $name (keys %CURR) {
    # if (!defined $newweights{$name}) {
    #   my $numnew = scalar(keys(%newweights));
    #   my $numold = scalar(keys(%CURR));
    #   die "Lost weight! mert reported fewer weights ($numnew) than we gave it ($numold) -- no key $name";
    # }
    print STDERR "DIFF: $name: $CURR{$name} -> $newweights{$name}\n";
    if (abs($CURR{$name} - $newweights{$name}) >= $minimum_required_change_in_weights) {
      $shouldstop = 0;
      last;
    }
  }

  &save_finished_step($finished_step_file, $run);

  if ($shouldstop) {
    print STDERR "None of the weights changed more than $minimum_required_change_in_weights. Stopping.\n";
    last;
  }

  my $firstrun;
  if ($prev_aggregate_nbl_size == -1) {
    $firstrun = 1;
  } else {
    $firstrun = $run - $prev_aggregate_nbl_size + 1;
    $firstrun = ($firstrun > 0) ? $firstrun : 1;
  }

  print "loading data from $firstrun to $run (prev_aggregate_nbl_size=$prev_aggregate_nbl_size)\n";
  $prev_feature_file = undef;
  $prev_score_file   = undef;
  $prev_init_file    = undef;
  for (my $i = $firstrun; $i <= $run; $i++) {
    if (defined $prev_feature_file) {
      $prev_feature_file = "${prev_feature_file},run${i}.${base_feature_file}";
    } else {
      $prev_feature_file = "run${i}.${base_feature_file}";
    }

    if (defined $prev_score_file) {
      $prev_score_file = "${prev_score_file},run${i}.${base_score_file}";
    } else {
      $prev_score_file = "run${i}.${base_score_file}";
    }
  }
  print "loading data from $prev_feature_file\n" if defined($prev_feature_file);
  print "loading data from $prev_score_file\n"   if defined($prev_score_file);
  print "loading data from $prev_init_file\n"    if defined($prev_init_file);
}

if (defined $allsorted) {
    safesystem ("\\rm -f $allsorted") or die;
}

safesystem("\\cp -f $mert_logfile run$run.$mert_logfile") or die;

if ($___RETURN_BEST_DEV) {
  my $bestit = 1;
  my $bestbleu = 0;
  my $evalout = "eval.out";
  for (my $i = 1; $i < $run; $i++) {
    safesystem("$mert_eval_cmd --reference " . join(",", @references) . " --candidate run$i.out 2> /dev/null 1> $evalout");
    open my $fh, '<', $evalout or die "Can't read $evalout : $!";
    my $bleu = <$fh>;
    chomp $bleu;
    if($bleu > $bestbleu) {
      $bestbleu = $bleu;
      $bestit = $i;
    }
    close $fh;
  }
  print "copying weights from best iteration ($bestit, bleu=$bestbleu) to moses.ini\n";
  create_config($weights_file, "./weights.final", get_featlist_from_file("run$bestit.weights"),
                $bestit, $bestbleu);
} else {
  create_config($weights_file, "./weights.final", $featlist, $run, $devbleu);
}

# just to be sure that we have the really last finished step marked
&save_finished_step($finished_step_file, $run);

#chdir back to the original directory # useless, just to remind we were not there
chdir($cwd);
print "Training finished at " . `date`;
} # end of local scope

sub get_weights_from_mert {
  my ($outfile, $logfile, $weight_count) = @_;
  my (%bestpoint, $devbleu);
  if ($___PAIRWISE_RANKED_OPTIMIZER || ($___PRO_STARTING_POINT && $logfile =~ /pro/)
          || $___BATCH_MIRA) {
    open my $fh, '<', $outfile or die "Can't open $outfile: $!";
    my @WEIGHT;
    for (my $i = 0; $i < $weight_count; $i++) { push @WEIGHT, 0; }
    my $sum = 0.0;
    while (<$fh>) {
      /^(.+) ([\-\.\de]+)/;
      $bestpoint{$1} = $2;
      $sum += abs($2);
    }
    close $fh;
    die "It seems feature values are invalid or unable to read $outfile." if $sum < 1e-09;

    $devbleu = "unknown";
    foreach (keys %bestpoint) { $bestpoint{$_} /= $sum; }

    if($___BATCH_MIRA) {
      open my $fh2, '<', $logfile or die "Can't open $logfile: $!";
      while(<$fh2>) {
        if(/Best BLEU = ([\-\d\.]+)/) {
          $devbleu = $1;
        }
      }
      close $fh2;
    }
  }
  return (\%bestpoint, $devbleu);
}

sub run_decoder {
    my ($featlist, $run, $need_to_normalize) = @_;
    my $filename_template = "run%d.best$___N_BEST_LIST_SIZE.out";
    my $filename = sprintf($filename_template, $run);
    my $lsamp_filename = undef;
    if ($___LATTICE_SAMPLES) {
      my $lsamp_filename_template = "run%d.lsamp$___LATTICE_SAMPLES.out";
      $lsamp_filename = sprintf($lsamp_filename_template, $run);
    }

    # user-supplied parameters
    print "params = $___DECODER_FLAGS\n";

    # parameters to set all model weights (to override moses.ini)
    if ($need_to_normalize) {
      print STDERR "Normalizing lambdas.\n";
      my $totlambda;
      map { $totlambda += abs($featlist->{$_}{value}) } keys(%$featlist);
      map { $featlist->{$_}{value} /= $totlambda } keys(%$featlist);
    }
    # moses now does not seem accept "-tm X -tm Y" but needs "-tm X Y"
    my %model_weights;
    foreach my $name (keys(%$featlist)) {
      $model_weights{$name} = "-$name" if !defined $model_weights{$name};
      $model_weights{$name} .= sprintf " %.6f", $featlist->{$name}{value};
    }
    my $decoder_config = "";
    print STDERR "DECODER_CFG = $decoder_config\n";
    print "decoder_config = $decoder_config\n";

    # run the decoder
    my $decoder_cmd;
    my $lsamp_cmd = "";
    if ($___LATTICE_SAMPLES) {
      $lsamp_cmd = " -lattice-samples $lsamp_filename $___LATTICE_SAMPLES ";
    }

    # if (defined $___JOBS && $___JOBS > 0) {
      # $decoder_cmd = "$moses_parallel_cmd $pass_old_sge -config $___CONFIG -inputtype $___INPUTTYPE -qsub-prefix mert$run -queue-parameters \"$queue_flags\" -decoder-parameters \"$___DECODER_FLAGS $decoder_config\" $lsamp_cmd -n-best-list \"$filename $___N_BEST_LIST_SIZE\" -input-file $___DEV_F -jobs $___JOBS -decoder $___DECODER > run$run.out";
    # } else {
    $decoder_cmd = "cat $___DEV_F | $___DECODER $___DECODER_FLAGS -config $___CONFIG $decoder_config -top-n $___N_BEST_LIST_SIZE 2> run$run.log | $JOSHUA/scripts/training/mira/feature_label_munger.pl | tee $filename | $JOSHUA/bin/extract-1best > run$run.out";
    # }

    safesystem($decoder_cmd) or die "The decoder died. CONFIG WAS $decoder_config \n";

    return ($filename, $lsamp_filename);
}


sub insert_ranges_to_featlist {
  my $featlist = shift;
  my $ranges = shift;

  $ranges = [] if !defined $ranges;

  # first collect the ranges from options
  my $niceranges;
  foreach my $range (@$ranges) {
    my $name = undef;
    foreach my $namedpair (split /,/, $range) {
      if ($namedpair =~ /^(.*?):/) {
        $name = $1;
        $namedpair =~ s/^.*?://;
        die "Unrecognized name '$name' in --range=$range"
          if !defined $ABBR2FULL{$name};
      }
      my ($min, $max) = split /\.\./, $namedpair;
      die "Bad min '$min' in --range=$range" if $min !~ /^-?[0-9.]+$/;
      die "Bad max '$max' in --range=$range" if $min !~ /^-?[0-9.]+$/;
      die "No name given in --range=$range" if !defined $name;
      push @{$niceranges->{$name}}, [$min, $max];
    }
  }

  # now populate featlist
  my $seen = undef;
  foreach my $name (keys %$featlist) {
    $seen->{$name} ++;
    my $min = 0.0;
    my $max = 1.0;
    if (defined $niceranges->{$name}) {
      my $minmax = shift @{$niceranges->{$name}};
      ($min, $max) = @$minmax if defined $minmax;
    }
    $featlist->{$name}{min} = $min;
    $featlist->{$name}{max} = $max;
  }
  return $featlist;
}

sub get_featlist_from_file {
  my $featlistfn = shift;

  my %featlist;

  # read feature list
  open my $fh, '<', $featlistfn or die "Can't read '$featlistfn': $!";
  my $nr = 0;
  my @errs = ();
  while (<$fh>) {
    $nr++;
    chomp;
    next if (/^#/ || /^\s*$/);   # skip blank lines and comments
    /^(\S+) (\S+)$/ || die "invalid feature: $_";
    my ($feature, $value) = ($1, $2);
    push @errs, "$featlistfn:$nr:Bad initial value of $feature: $value\n"
      if $value !~ /^[+-]?[0-9.\-e]+$/;
    $featlist{$feature}{value} = $value;
  }
  close $fh;

  if (scalar @errs) {
    warn join("", @errs);
    exit 1;
  }
  return \%featlist;
}


sub get_order_of_scores_from_nbestlist {
  # read the first line and interpret the ||| label: num num num label2: num ||| column in nbestlist
  # return the score labels in order
  my $fname_or_source = shift;
  # print STDERR "Peeking at the beginning of nbestlist to get order of scores: $fname_or_source\n";
  open my $fh, $fname_or_source or die "Failed to get order of scores from nbestlist '$fname_or_source': $!";
  my $line = <$fh>;
  close $fh;
  die "Line empty in nbestlist '$fname_or_source'" if !defined $line;
  my ($sent, $hypo, $scores, $total) = split /\|\|\|/, $line;
  $scores =~ s/^\s*|\s*$//g;
  die "No scores in line: $line" if $scores eq "";

  # Replace equals signs with ": " (converting from Joshua format to Moses)
  $scores =~ s/=/: /g;

  my @order = ();
  my $label = undef;
  my $sparse = 0; # we ignore sparse features here
  foreach my $tok (split /\s+/, $scores) {
    if ($tok =~ /.+:/) {
      $sparse = 1;
    } elsif ($tok =~ /^-?[-0-9.\-e]+$/) {
      if (!$sparse) {
        # a score found, remember it
        die "Found a score but no label before it! Bad nbestlist '$fname_or_source'!"
          if !defined $label;
        push @order, $label;
      }
      $sparse = 0;
    } else {
      die "Not a label, not a score '$tok'. Failed to parse the scores string: '$scores' of nbestlist '$fname_or_source'";
    }
  }
  print STDERR "The decoder returns the scores in this order: @order\n";
  return @order;
}

sub create_config {
  # TODO: too many arguments. you might want to consider using hashes
  my $infn                = shift; # source config
  my $outfn               = shift; # where to save the config
  my $featlist            = shift; # the lambdas we should write
  my $iteration           = shift; # just for verbosity
  my $bleu_achieved       = shift; # just for verbosity

  my %P; # the hash of all parameters we wish to override

  # first convert the command line parameters to the hash
  # ensure local scope of vars
  # {
  #   my $parameter = undef;
  #   print "Parsing --decoder-flags: |$___DECODER_FLAGS|\n";
  #   $___DECODER_FLAGS =~ s/^\s*|\s*$//;
  #   $___DECODER_FLAGS =~ s/\s+/ /;
  #   foreach (split(/ /, $___DECODER_FLAGS)) {
  #     if (/^\-([^\d].*)$/) {
  #       $parameter = $1;
  #       $parameter = $ABBR2FULL{$parameter} if defined($ABBR2FULL{$parameter});
  #     } else {
  #       die "Found value with no -paramname before it: $_"
  #           if !defined $parameter;
  #       push @{$P{$parameter}}, $_;
  #     }
  #   }
  # }

  # First delete all weights params from the input, we're overwriting them.
  # Delete both short and long-named version.
  foreach my $name (keys(%$featlist)) {
    delete($P{$name});
    # delete($P{$ABBR2FULL{$name}});
  }

  # Convert weights to elements in P
  foreach my $name (keys(%$featlist)) {
    my $val = $featlist->{$name}{value};
    $name = defined $ABBR2FULL{$name} ? $ABBR2FULL{$name} : $name;
    # ensure long name
    push @{$P{$name}}, $val;
  }

  # create new moses.ini decoder config file by cloning and overriding the original one
  open my $ini_fh, '<', $infn or die "Can't read $infn: $!";
  delete($P{"config"}); # never output
  print "Saving new config to: $outfn\n";

  open my $out, '>', $outfn or die "Can't write $outfn: $!";
  # print $out "# MERT optimized configuration\n";
  # print $out "# decoder $___DECODER\n";
  # print $out "# BLEU $bleu_achieved on dev $___DEV_F\n";
  # print $out "# We were before running iteration $iteration\n";
  # print $out "# finished ".`date`;

  while(my $line = <$ini_fh>) {

    if ($line =~ /^\#/ || $line =~ /^\s+$/) {
      print $out $line;
      next;
    }

    my ($parameter, $value) = split(' ', $line);
    $parameter = $ABBR2FULL{$parameter} if defined($ABBR2FULL{$parameter});

    # change parameter, if new values
    if (defined($P{$parameter})) {
      # write new values
      foreach (@{$P{$parameter}}) {
        print $out "$parameter $_\n";
      }
      delete($P{$parameter});
    } else {
      print $out $line;
    }
  }

  # write all additional parameters
  foreach my $parameter (keys %P) {
    foreach (@{$P{$parameter}}) {
      print $out "$parameter $_\n";
    }
  }

  close $ini_fh;
  close $out;
  print STDERR "Saved: $outfn\n";
}

sub safesystem {
  print STDERR "Executing: @_\n";
  system(@_);
  if ($? == -1) {
      warn "Failed to execute: @_\n  $!";
      exit(1);
  } elsif ($? & 127) {
      printf STDERR "Execution of: @_\n  died with signal %d, %s coredump\n",
          ($? & 127),  ($? & 128) ? 'with' : 'without';
      exit(1);
  } else {
    my $exitcode = $? >> 8;
    warn "Exit code: $exitcode\n" if $exitcode;
    return ! $exitcode;
  }
}

sub ensure_full_path {
  my $PATH = shift;
  $PATH =~ s/\/nfsmnt//;
  return $PATH if $PATH =~ /^\//;

  my $dir = Cwd::getcwd();
  $PATH = File::Spec->catfile($dir, $PATH);
  $PATH =~ s/[\r\n]//g;
  $PATH =~ s/\/\.\//\//g;
  $PATH =~ s/\/+/\//g;
  my $sanity = 0;
  while($PATH =~ /\/\.\.\// && $sanity++ < 10) {
    $PATH =~ s/\/+/\//g;
    $PATH =~ s/\/[^\/]+\/\.\.\//\//g;
  }
  $PATH =~ s/\/[^\/]+\/\.\.$//;
  $PATH =~ s/\/+$//;
  $PATH =~ s/\/nfsmnt//;
  return $PATH;
}

sub submit_or_exec {
  my ($cmd, $stdout, $stderr) = @_;
  print STDERR "exec: $cmd\n";
  if (defined $___JOBS && $___JOBS > 0) {
    safesystem("$qsubwrapper $pass_old_sge -command='$cmd' -queue-parameter=\"$queue_flags\" -stdout=$stdout -stderr=$stderr" )
      or die "ERROR: Failed to submit '$cmd' (via $qsubwrapper)";
  } else {
    safesystem("$cmd > $stdout 2> $stderr") or die "ERROR: Failed to run '$cmd'.";
  }
}

sub create_extractor_script() {
  my ($cmd, $outdir) = @_;
  my $script_path = File::Spec->catfile($outdir, "extractor.sh");

  open my $out, '>', $script_path
      or die "Couldn't open $script_path for writing: $!\n";
  print $out "#!/bin/bash\n";
  print $out "cd $outdir\n";
  print $out "$cmd\n";
  close $out;

  `chmod +x $script_path`;

  return $script_path;
}

sub save_finished_step {
  my ($filename, $step) = @_;
  open my $fh, '>', $filename or die "$filename: $!";
  print $fh $step . "\n";
  close $fh;
}

# It returns a config for mert/extractor.
sub setup_reference_length_type {
  if (($___CLOSEST + $___AVERAGE + $___SHORTEST) > 1) {
    die "You can specify just ONE reference length strategy (closest or shortest or average) not both\n";
  }

  if ($___SHORTEST) {
    return " reflen:shortest";
  } elsif ($___AVERAGE) {
    return " reflen:average";
  } elsif ($___CLOSEST) {
    return " reflen:closest";
  } else {
    return "";
  }
}

sub setup_case_config {
  if ($___NOCASE) {
    return " case:false";
  } else {
    return " case:true";
  }
}

sub is_mac_osx {
  return ($^O eq "darwin") ? 1 : 0;
}
