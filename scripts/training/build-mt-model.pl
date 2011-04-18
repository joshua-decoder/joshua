#!/usr/bin/perl

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use Cwd;
use CachePipe;

my (@CORPORA,$TUNE,$DEV,$FR,$EN,$LMFILE,$FIRST_STEP,$LAST_STEP);
my $MAXLEN = 50;
my $DO_SUBSAMPLE = ''; # default false
my $SCRIPTDIR = "$ENV{JOSHUA}/scripts";
my $TOKENIZER = "$SCRIPTDIR/training/penn-treebank-tokenizer.perl";
my $MOSES_TRAINER = "/home/hltcoe/airvine/bin/moses/tools/moses-scripts/scripts-20100922-0942/training/train-factored-phrase-model.perl";
my $MERTCONFDIR = "$ENV{JOSHUA}/scripts/training/templates/mert";
my $SRILM = "$ENV{SRILM}/bin/i686-m64/ngram-count";
my $STARTDIR;
my $RUNDIR = $STARTDIR = getcwd;
my $GRAMMAR = "hiero";
my $HADOOP = $ENV{HADOOP};

# for hadoop java subprocesses (heap amount)
# you really just have to play around to find out how much is enough 
my $HADOOP_MEM = "8G";  
my $JOSHUA_MEM = "32g";

my $options = GetOptions(
  "corpus=s" => \@CORPORA,
  "tune=s"   => \$TUNE,
  "dev=s"    => \$DEV,
  "fr=s"     => \$FR,
  "en=s"     => \$EN,
  "rundir=s" => \$RUNDIR,
  "lmfile=s" => \$LMFILE,
  "maxlen=i" => \$MAXLEN,
  "tokenizer=s" => \$TOKENIZER,
  "subsample!" => \$DO_SUBSAMPLE,
  "grammar=s" => \$GRAMMAR,
  "first-step=s" => \$FIRST_STEP,
  "last-step=s" => \$LAST_STEP,
);

$| = 1;

my $cachepipe = new CachePipe();

## Sanity Checking ###################################################

if (@CORPORA == 0) {
  print "* FATAL: need at least one training corpus (--corpus)\n";
  exit 1;
}

if (! defined $TUNE) {
  print "* FATAL: need a tuning set (--tune)\n";
  exit 1;
}

if (! defined $DEV) {
  print "* FATAL: need a dev set (--dev)\n";
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

my $OOV = ($GRAMMAR eq "samt") ? "OOV" : "X";

mkdir $RUNDIR unless -d $RUNDIR;
chdir($RUNDIR);

if ($FIRST_STEP) {
  if (eval { goto $FIRST_STEP }) {
	print "* Skipping to step $FIRST_STEP\n";
	goto $FIRST_STEP;
  } else {
	print "* No such step $FIRST_STEP\n";
	exit 1;
  }
}

## STEP 1: filter and preprocess corpora #############################

sub prepare_data {
  my ($label,$corpora,$maxlen) = @_;

  mkdir $label unless -d $label;

  # copy the data from its original location to our location
  foreach my $lang ($EN,$FR) {
	my @files = map { "$_.$lang" } @$corpora;
	my $files = join(" ",@files);
	$cachepipe->cmd("$label-copy-$lang",
					"cat $files > $label/$label.$lang",
					@files, "$label/$label.$lang");
  }

  # tokenize the data
  foreach my $lang ($EN,$FR) {
	$cachepipe->cmd("$label-tokenize-$lang",
					"$SCRIPTDIR/training/scat $label/$label.$lang | $TOKENIZER -l $lang > $label/$label.$lang.tok 2> /dev/null",
					"$label/$label.$lang", "$label/$label.$lang.tok"
		);
  }

  my $affix = "";
  if ($maxlen and $label eq "train") {
	# trim training data
	$cachepipe->cmd("train-trim",
					"$SCRIPTDIR/training/trim_parallel_corpus.pl $label/$label.$EN.tok $label/$label.$FR.tok $maxlen > $label/$label.$EN.tok.$maxlen 2> $label/$label.$FR.tok.$maxlen",
					"$label/$label.$EN.tok", "$label/$label.$FR.tok",
					"$label/$label.$EN.tok.$maxlen", "$label/$label.$FR.tok.$maxlen",
		);
	$affix = ".$maxlen";
  }

  # lowercase
  foreach my $lang ($EN,$FR) {
	$cachepipe->cmd("$label-lowercase-$lang",
 					"cat $label/$label.$lang.tok$affix | $SCRIPTDIR/lowercase.perl > $label/$label.$lang.tok$affix.lc",
					"$label/$label.$lang.tok$affix",
					"$label/$label.$lang.tok$affix.lc");
  }
}

# prepare the training data
prepare_data("train",\@CORPORA,$MAXLEN);

# prepare the tuning and development data
prepare_data("tune",[$TUNE]);
prepare_data("dev",[$DEV]);

# subsample
if ($DO_SUBSAMPLE) {
  print "\n\n** DO_SUBSAMPLE UNIMPLEMENTED\n\n";
  exit 1;

  mkdir("train/subsampled");

  $cachepipe->cmd("subsample-manifest",
				  "echo train/corpus > train/subsampled/manifest",
				  "train/subsampled/manifest");

  $cachepipe->cmd("subsample-testdata",
				  "cat $TUNE.$FR $DEV.$FR > train/subsampled/test-data",
				  "$TUNE.$FR", "$DEV.$FR", "train/subsampled/test-data");

  $cachepipe->cmd("subsample",
				  "java -Xmx4g -Dfile.encoding=utf8 -cp $ENV{JOSHUA}/bin:$ENV{JOSHUA}/lib/commons-cli-2.0-SNAPSHOT.jar joshua.subsample.Subsampler -e $EN.tok.$MAXLEN -f $FR.tok.$MAXLEN -epath train/ -fpath train/ -output train/subsampled/subsampled.$MAXLEN -ratio 1.04 -test train/subsampled/test-data -training train/subsampled/manifest",
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
					"ln -sf train.$lang.tok.$MAXLEN.lc train/corpus.$lang",
					"train/corpus.$lang");
  }
}

# alignment
$cachepipe->cmd("giza",
				"rm -f train/corpus.0-0.*; $MOSES_TRAINER -root-dir giza -e $EN -f $FR -corpus train/corpus -first-step 1 -last-step 3 > giza.log 2>&1",
				"train/corpus.$FR",
				"train/corpus.$EN",
				"giza/model/aligned.grow-diag-final");


my ($frfile,$enfile);
$frfile = "train/corpus.$FR";
if ($GRAMMAR eq "samt") {

#   cachecmd parse "~mpost/code/cdec/vest/parallelize.pl -j 20 -- java -cp /home/hltcoe/mpost/code/berkeleyParser edu.berkeley.nlp.PCFGLA.BerkeleyParser -gr /home/hltcoe/mpost/code/berkeleyParser/eng_sm5.gr <subsampled/subsample.$pair.$maxlen.$en.tok | sed s/^\(/\(TOP/ >subsampled/subsample.$pair.$maxlen.$en.parsed" subsampled/subsample.$pair.$maxlen.$en.{tok,parsed}

  $enfile = "train/corpus.$EN.parsed";

} else {
  $enfile = "train/corpus.$EN";
}

# create the input file
$cachepipe->cmd("thrax-input-file",
				"paste $frfile $enfile giza/model/aligned.grow-diag-final | perl -pe 's/\t/ ||| /g' | grep -v '(())' > train/thrax-input-file",
				$frfile, $enfile, "giza/model/aligned.grow-diag-final",
				"train/thrax-input-file");

# put the hadoop files in place
my $thraxdir = "pipeline-$FR-$EN-$GRAMMAR";
$cachepipe->cmd("thrax-prep",
				"$HADOOP/bin/hadoop fs -rmr $thraxdir; $HADOOP/bin/hadoop fs -mkdir $thraxdir; $HADOOP/bin/hadoop fs -put train/thrax-input-file $thraxdir/input-file",
				"train/thrax-input-file", 
				"grammar.gz");

$cachepipe->cmd("thrax-config",
			   "cp $ENV{JOSHUA}/scripts/training/templates/thrax-$GRAMMAR.conf .; echo input-file $thraxdir/input-file >> thrax-$GRAMMAR.conf",
			   "$ENV{JOSHUA}/scripts/training/templates/thrax-$GRAMMAR.conf",
			   "thrax-$GRAMMAR.conf");

$cachepipe->cmd("thrax-run",
				"$HADOOP/bin/hadoop jar $ENV{THRAX}/bin/thrax.jar -D mapred.child.java.opts='-Xmx$HADOOP_MEM' thrax-$GRAMMAR.conf $thraxdir > thrax.log 2>&1",
				"train/thrax-input-file");

$cachepipe->cmd("thrax-get",
				"rm -f grammar grammar.gz; $HADOOP/bin/hadoop fs -getmerge $thraxdir/final/ grammar; gzip -9 grammar",
				"train/thrax-input-file",
				"grammar.gz");

## MERT ##############################################################
MERT:

if (! defined $LMFILE) {
  $LMFILE="lm.gz";
  $cachepipe->cmd("srilm",
				  "$SRILM -interpolate -kndiscount -order 5 -text train/corpus.en -lm lm.gz",
				  $LMFILE);
}

# filter the development grammar
$cachepipe->cmd("filter-tune",
				"$SCRIPTDIR/training/scat grammar.gz | $ENV{THRAX}/scripts/filter_rules.sh 12 tune/tune.$FR.tok.lc | gzip -9 > tune/grammar.filtered.gz",
				"grammar.gz", 
				"tune/tune.$FR.tok.lc", 
				"tune/grammar.filtered.gz");

$cachepipe->cmd("glue-tune",
				"$SCRIPTDIR/training/scat tune/grammar.filtered.gz | $ENV{THRAX}/scripts/create_glue_grammar.sh thrax-$GRAMMAR.conf > tune/grammar.glue",
				"tune/grammar.filtered.gz",
				"tune/grammar.glue");

mkdir("mert") unless -d "mert";
my @mertfiles = qw(decoder_command joshua.config mert.config params.txt);
foreach my $file (@mertfiles) {
  open FROM, "$MERTCONFDIR/$file" or die;
  open TO, ">mert/$file" or die;
  while (<FROM>) {
	s/<FR>/$FR/g;
	s/<LMFILE>/$LMFILE/g;
	s/<MEM>/$JOSHUA_MEM/g;
	s/<GRAMMAR>/$GRAMMAR/g;
	s/<OOV>/$OOV/g;

	print TO;
  }
}
chmod(0755,"mert/decoder_command");

$cachepipe->cmd("mert",
				"java -d64 -cp $ENV{JOSHUA}/bin joshua.zmert.ZMERT -maxMem 1500 mert/mert.config",
				map { "mert/$_" } @mertfiles);


