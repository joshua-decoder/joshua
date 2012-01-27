#!/usr/bin/perl -w
#$ -S /usr/bin/perl

# Author: Damianos Karakos <damianos@jhu.edu>

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use Cwd;
# use POSIX qw[ceil];
# use List::Util qw[max min sum];
# use File::Temp qw/ :mktemp /;

my $JOSHUA = $ENV{JOSHUA};

my $script = "$JOSHUA/scripts/support/filtering_script.sh";

my $proc_id = $$;
my $hostname = `hostname`;
chomp($hostname);
my $tmp_dir = "tmpdir.$hostname.$proc_id";


if(@ARGV == 0)
{
    die "Usage: $0 --corpus=<corpus> --grammar=<grammar file> --n=<number of pieces> --output_grammar=<output grammar file> --lines=<number of lines of grammar> --fast --ngrams=<maximum n n-gram to compare to\n";
}

my ($corpus, $grammar_file, $num_pieces, $output_grammar, $num_lines, $fast, $ngrams) = ("","",0,"",0,0,12);

my $retval = GetOptions(
    "grammar=s"         => \$grammar_file,
    "n=s"               => \$num_pieces,
    "output_grammar=s"  => \$output_grammar,
    "corpus=s"          => \$corpus,
    "lines=s"       => \$num_lines,
    "fast!"      => \$fast,
    "ngrams=i"   => \$ngrams,
);

if (! $retval) {
    print "Invalid usage, quitting\n";
    exit 1;
}

print STDERR "Grammar file: $grammar_file\n";
print STDERR "Number of pieces: $num_pieces\n";
print STDERR "Output grammar: $output_grammar\n";
print STDERR "Corpus: $corpus\n";
print STDERR "Number of lines in grammar: $num_lines\n" if($num_lines > 0);

if($num_lines == 0)
{
    open F, "gzip -cdf $grammar_file |" or die "ERROR: Cannot open $grammar_file\n";

    while(<F>)
    {
        $num_lines++;
    }
    close F;

    print STDERR "Found $num_lines in $grammar_file\n";
}


my $num_lines_per_piece = $num_lines/$num_pieces;

if($num_lines_per_piece != int($num_lines_per_piece))
{
    $num_lines_per_piece = int($num_lines_per_piece+1);
}

mkdir $tmp_dir;

my $grammar_basename = basename($grammar_file);

open F, "gzip -cdf $grammar_file |" or die "ERROR: Cannot open $grammar_file\n";

my $lines_remaining = $num_lines;

my $actual_num_pieces = 0;      # This is needed because we are using the ceiling of num_lines/num_pieces, so the true number of pieces may be less

foreach my $i (1..$num_pieces)
{
    $actual_num_pieces++;
    my $grammar_piece = "$tmp_dir/$grammar_basename.$i";
    my $filtered_grammar_piece = "$grammar_piece.filtered.gz";
    
    open G, "| gzip -c > $grammar_piece.gz" or die "ERROR: Cannot write to $grammar_piece.gz\n";
    
    foreach(1..$num_lines_per_piece)
    {
        my $grammar_line = <F>;
        print G $grammar_line;
        $lines_remaining--;
        last if($lines_remaining == 0);
    }
    close G;

    my $logfile = "$tmp_dir/log.filtering.$i";
    
    &submit_job($script, $logfile, "$grammar_piece.gz", $corpus, $filtered_grammar_piece);
    
    last if($lines_remaining == 0);
}

$num_pieces = $actual_num_pieces;

#### Monitor the jobs and merge the resulting files if all jobs are done

my $num_finished = 0;

print STDERR "Waiting for grid jobs to finish\n";

while($num_finished < $num_pieces)
{
    # print STDERR "Number of finished jobs: $num_finished\n";
    $num_finished = 0;
    foreach my $i (1..$num_pieces)
    {
        open F, "$tmp_dir/log.filtering.$i" or next;
        # print STDERR "Checking $tmp_dir/log.filtering.$i\n";
        my @log_lines = <F>;
        
        # print STDERR $log_lines[-1];
        
        if((@log_lines > 0) && ($log_lines[-1] =~ m/skipped/i))
        {
            $num_finished++;
        }
    }
    sleep(1);
}

print STDERR "Grid jobs are done -- merging the filtered files\n";


#### We will merge the resulting filtered files and save them in the designated location

open G, "| gzip -c > $output_grammar" or die "ERROR: Cannot write to $output_grammar\n";

foreach my $i (1..$num_pieces)
{
    my $grammar_piece = "$tmp_dir/$grammar_basename.$i";
    my $filtered_grammar_piece = "$grammar_piece.filtered";

    open F, "gzip -cdf $filtered_grammar_piece |" or die "ERROR: Cannot open $filtered_grammar_piece\n";
    while(<F>)
    {
        print G $_;
    }
    close F;
}

# remove the temporary directory
system("rm -rf $tmp_dir");
    

sub submit_job
{
    my ($script, $logfile, $grammar_piece, $corpus, $filtered_grammar_piece) = @_;

    unlink($logfile);
    system("qsub -cwd -j y -o $logfile -v JOSHUA=$JOSHUA $script $grammar_piece $corpus $filtered_grammar_piece $fast $ngrams");
}

