# Matt Post <post@cs.jhu.edu>

package CachePipe;

use strict;
use Exporter;
use vars qw|$VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS|;

# our($lexicon,%rules,%deps,%PARAMS,$base_measure);

@ISA = qw|Exporter|;
@EXPORT = qw|signature cache cache_from_file sha1hash|;
@EXPORT_OK = ();

use strict;
use Carp qw|croak|;
use warnings;
# use threads;
# use threads::shared;
use POSIX qw|strftime|;
use List::Util qw|reduce min shuffle sum|;
use IO::Handle;
# use Digest::SHA1;
# use Memoize;

# constructor
sub new {
  my $class = shift;
  my %params = @_;
  # default values
  my $self = { 
	dir       => ".cachepipe",
	basedir   => $ENV{PWD},
	retval    => 0,             # expected return value
	running   => "",            # currently running command
	lastrun   => "",            # last command run
	qsub_args => "",            # qsub args
	omit_cmd  => 1,             # whether to omit the command from the signature
	email   => undef,
  };

  map { $self->{$_} = $params{$_} } keys %params;
  bless($self,$class);

  STDERR->autoflush(1);

  return $self;
}

sub omit_cmd {
  my ($self) = @_;
  $self->{omit_cmd} = 1;
}

sub include_cmd {
  my ($self) = @_;
  $self->{omit_cmd} = 0;
}

# cleanup
sub cleanup {
  my ($self) = @_;

  if ($self->{running}) {
	my $file = "$self->{dir}/$self->{running}/running";
	print "CLEANUP: removing file '$file'\n";
	unlink($file);
  }
}

# static version of cmd()
sub cache {
  my @args = @_;
  my $pipe = new CachePipe();
  $pipe->cmd(@args);
}

# static version that reads args from file
sub cache_from_file {
  my ($file) = @_;

  my @args;
  open READ, $file or die;
  while (my $line = <READ>) {
	chomp($line);
	push(@args,$line);
  }
  close(READ);

  # print join("\n",@args) . $/;

  my $pipe = new CachePipe();
  $pipe->cmd(@args);
}

sub build_signatures {
  my ($self,$cmd,@deps) = @_;

  # print STDERR "CMD: $cmd\n";
  # map { print STDERR "DEP: $_\n" } @deps;

  if ($self->{omit_cmd}) {
	$cmd = "[omitted]";
  }

  # remove blocked off portion
  $cmd =~ s/<<<.*?>>>//g;

  # generate git-style signatures for input and output files
  my @sigs = map { sha1hash($_) } @deps;

  # walk through the command line and substitute signatures for file names
  my %filehash;
  for my $i (0..$#deps) {
	my $file = $deps[$i];
	$filehash{$file} = $sigs[$i];
  }
  my @tokens = split(' ',$cmd);
  for my $i (0..$#tokens) {
	my $token = $tokens[$i];
	if (exists $filehash{$token}) {
	  $tokens[$i] = $filehash{$token};
	}
  }
  # print STDERR "CMDSIG($cmd) = " . join(" ",@tokens) . $/;
  my $cmdsig = join(" ",@tokens);

  # generate a signature of the concatenation of the command and the
  # dependency signatures
  # TODO: 
  my $new_signature = signature(join(" ", $cmdsig, @sigs));
  
  return ($new_signature,$cmdsig,@sigs);
}

# returns the STDOUT of the last-run command
sub stdout {
  my ($self) = @_;
  
  my $stdout = "";
  if ($self->{lastrun} ne "") {
	my $namedir = $self->{dir} . "/" . $self->{lastrun};

	chomp($stdout = `cat $namedir/out`);

	$self->mylog("[$self->{lastrun}] retrieved cached result => $stdout");
  }
  
  return $stdout;
}

# Use cases:
# - Run the given command and cache the results if successful
# - Rerun a previous command, cache the results upon success
# - Recache an old command
# - Cache a new command without running

# Runs a command (if required)
# name: the (unique) name assigned to this command
# input_deps: an array ref of input file dependencies
# cmd: the complete command to run
# output_deps: an array ref of output file dependencies
#
# return code 1: command was re-run
# return code 0: command was cached
sub cmd {
  my ($self,$name,@args) = @_;

  die "no name provided" unless $name ne "";

  $self->{lastrun} = $name;

  # Process command-line arguments

  my ($cmd,@deps);
  my ($cache_only) = 0;
  my ($rerun) = 0;
  my ($use_qsub) = 0;
  while (my $arg = shift @args) {
	if ($arg =~ /^--/) {
	  $arg =~ s/^--//;
	  if ($arg eq "cache-only") {
		# print STDERR "* argument: --$arg\n";
		$cache_only = 1;
	  } elsif ($arg eq "rerun") {
		$rerun = 1;
	  } elsif ($arg eq "qsub") {
		$use_qsub = 1;
	  } else {
		print STDERR "* FATAL: CachePipe: unknown argument: --$arg\n";
		exit 1;
	  }
	} else {
	  $cmd = $arg;
	  @deps = @args;
	  last;
	}
  }

  # the directory where cache information is written
  my $dir = $self->{dir};
  my $namedir = "$dir/$name";

  # This check causes more trouble than it's worth...
  # if (-e "$namedir/running") {
  # 	$self->mylog("[$name] already running, QUITTING...");
  # 	exit 1;
  # }

  if (-e "$namedir/cache-only") {
	$self->mylog("[$name] 'cache-only' file exists, caching...");
	$cache_only = 1;
  }

  # define this to avoid complaints about comparisons to undefined strings
  my $old_signature = "";
  my @old_sigs;

  if (! -d $dir) {
	# if no caching has ever been done

	system("mkdir","-p",$namedir);

  } elsif (! -d $namedir)  {
	# otherwise, if this command hasn't yet been cached...

	mkdir($namedir);

  } elsif (! -e "$namedir/signature") { 
	# otherwise if the signature file doesn't exist...

  } else {
	# everything exists, but we need to check whether anything has changed
	
	open(READ, "$namedir/signature") 
		or die "no such file '$namedir/signature'";
	chomp($old_signature = <READ>);

	# if we're caching, and nothing was provided, that means to
	# recache the old command, so we need to read in what that command
	# was, along with the dependencies
	if (($cache_only or $rerun) and ! defined $cmd) {
	  chomp($cmd = <READ>);
	  $cmd =~ s/^.* CMD //;
	  while (my $line = <READ>) {
		my @tokens = split(' ',$line);
		push(@old_sigs, $tokens[0]);
		push(@deps,$tokens[-1]);
	  }

	# otherwise, we're caching, but a new command and set of
	# dependencies was provided, so we need to use those
	} else {
	  # throw away command signature line
	  <READ>;
	  # read in file dependency signatures
	  while (my $line = <READ>) {
		my @tokens = split(' ',$line);
		push(@old_sigs, $tokens[0]);
	  }
	}
	close (READ);
  }

  if (! defined $cmd and -e "$namedir/cmd") {
	chomp($cmd = `cat $namedir/cmd`);
  }

  # if caching was requested and no new command provided, make sure we
  # found an old command
  if ($cache_only and ! defined $cmd) {
	$self->mylog("[$name] FATAL: requested cache of old command, but no old command found");
	exit 1;
  }

  # if a rerun was requested, but no old signature found, error
  if ($rerun and ! defined $cmd) {
	$self->mylog("[$name] FATAL: requested rerun, but no old command found");
	exit 1;
  }

  my ($new_signature,$cmdsig,@sigs) = $self->build_signatures($cmd,@deps);

  # remove markers for portions of the command not incorporated in the
  # signature; this is the actual string that gets executed
  my $runcmd = $cmd;
  $runcmd =~ s/<<<|>>>//g;

  # fill up @old_sigs so its as long as @sigs (to avoid complaints
  # about undefined comparisons)
  while (@old_sigs < @sigs) {
	push(@old_sigs, "");
  }

  if ($old_signature ne $new_signature) {

	my $message = ($cache_only) ? "recaching" : "rebuilding";

	$self->mylog("[$name] $message...");
	for my $i (0..$#deps) {
	  my $dep = $deps[$i];

	  if (-e $dep) {
		my $diff = ($sigs[$i] eq $old_sigs[$i]) ? "" : "[CHANGED]";
		$self->mylog("  dep=$dep $diff");
	  } else {
		$self->mylog("  dep=$dep [NOT FOUND]");
	  }
	}
	$self->mylog("  cmd=$runcmd");

	if ($cache_only) {

	  $self->write_signature($namedir,$cmd,@deps);

	  return 0;

	} else {
	  system("touch $namedir/running");
	  $self->{running} = $name;

	  write_cmd($namedir,$cmd);

	  my $start_time = time();

	  # run the command
	  # redirect stdout and stderr

	  open OLDOUT, ">&", \*STDOUT or die;
	  open OLDERR, ">&", \*STDERR or die;

	  open(STDOUT,">$namedir/out") or die;
	  open(STDERR,">$namedir/err") or die;

	  if ($use_qsub) {
		system("qsub $self->{qsub_args} -cwd $namedir/cmd");
	  } else {
		system("/bin/bash","-c",$runcmd);
	  }

	  close(STDOUT);
	  close(STDERR);

	  open(STDOUT,">&", \*OLDOUT);
	  open(STDERR,">&", \*OLDERR);

	  my $retval = $? >> 8;

	  if ($retval == $self->{retval}) {
		$self->write_signature($namedir,$cmd,@deps);

		unlink("$namedir/running");
		$self->{running} = "";

		my $stop_time = time();

		my $seconds = ($stop_time - $start_time);
		my $duration = pretty_print_time($seconds);
		$self->mylog("  took $seconds seconds ($duration)");
		system("echo $duration > $namedir/runtime");

		return 1;

	  } else {
		$self->mylog("  JOB FAILED (return code $retval)");
		unlink("$namedir/running");
		system("cat $namedir/err");
		exit(-1);
	  }
	}

  } else {
	$self->mylog("[$name] cached, skipping...");

	return 0;
  }
}

# This function determines whether a command needs to be re-run.
# There are two use cases:
# - cached(step): returns true if the step was found and its signature
#   is the same as the old one (this is a short-cut for use case #2)
# - cached(step,cmd,deps): returns true if the step with given command
#   and dependencies is the same as the cached one
sub cached {
  my ($self,$name,$cmd,@deps) = @_;

  die "no name provided" unless $name ne "";

  # the directory where cache information is written
  my $dir = $self->{dir};
  my $namedir = "$dir/$name";


  # we definitely need to run if there's no previous run
  if (! -e "$namedir/signature") {
	return 1;
  } else {

	# check whether anything has changed
	open(READ, "$namedir/signature") 
		or die "no such file '$namedir/signature'";
	chomp(my $old_signature = <READ>);
	close(READ);

	my ($new_signature) = $self->build_signatures($cmd,@deps);

	# if the old signature is different from the new one, we need to re-run
	if ($old_signature ne $new_signature) {
	  return 1;
	}
  }

  # if we get here, the signature of the new command and old are the
  # same, and we don't need to re-run
  return 0;
}

sub write_cmd {
  my ($namedir,$cmd) = @_;

  open(WRITE, ">$namedir/cmd");
  print WRITE "$cmd\n";
  close(WRITE);
}

sub write_signature {
  my ($self,$namedir,$cmd,@deps) = @_;

  my ($new_signature,$cmdsig,@sigs) = $self->build_signatures($cmd,@deps);

  # regenerate signature
  open(WRITE, ">$namedir/signature") or die "FATAL: can't write to '$namedir/signature'";
  print WRITE $new_signature . $/;
  
  print WRITE "$cmdsig CMD $cmd\n";
  map {
	print WRITE "$sigs[$_] DEPENDENCY $deps[$_]\n";
  } (0..$#sigs);
  close(WRITE);

  # generate timestamp
  open(WRITE, ">$namedir/timestamp");
  print WRITE time() . $/;
  close(WRITE);
}


# Generates a GIT-style signature of a file.  Thanks to
# http://stackoverflow.com/questions/552659/assigning-git-sha1s-without-git
sub sha1hash {
  my ($arg) = @_;

  if ($arg =~ /^ENV:/) {
	my (undef,$env) = split(':',$arg);
	my $content = $ENV{$env} || time();
	return signature($content);
  } else {
	return file_signature($arg);
  }

  return signature($arg);
}

# Generates a GIT-style signature of a string *without* loading the whole file into memory 
sub file_signature {
  my ($file) = @_;

  if (-e $file) {
	my $size = (stat($file))[7];
	# my $header = "blob $size\\0";
	# my $sha1 = new Digest::SHA1();
	# $sha1->add($header);
	# $sha1->addfile(FILE);
	# my $hash = $sha1->hexdigest();
	# return $hash;
	chomp(my $sha1 = `(echo -ne "blob $size\\0"; cat $file) | sha1sum -b | awk '{print \$1}'`);

	return $sha1;
  } else {
	# if the file doesn't exist, return a signature guaranteed to be
	# unique, triggering a re-run
	my $time = time();
	chomp(my $sha1 = `echo $time | sha1sum -b | awk '{print \$1}'`);

	return $sha1;
  }
}


# Generates a GIT-style signature of a string
sub signature {
  my ($content) = @_;

  my $length;
  {
	use bytes;
	$length = length($content);
  }

  my $git_blob = 'blob' . ' ' . length($content) . "\0" . $content;
  my $tmpfile = ".tmp.$$";
  open OUT, ">$tmpfile" or die;
  print OUT $git_blob;
  close(OUT);
  chomp(my $sha1 = `cat $tmpfile | sha1sum -b | awk '{ print \$1 }'`);
  unlink($tmpfile);
  return $sha1;
}

sub mylog {
	my ($self,$msg) = @_;

	print STDERR $msg . $/;
        
}

sub get_status {
  my ($self,$name) = @_;

  my $dir = $self->{dir};
  my $namedir = "$dir/$name";

  my $status = (-e "$namedir/status")
	  ? `cat $namedir/status`
	  : "";
  chomp($status);
	
  return $status;
}

sub pretty_print_time {
  my $seconds = shift;

  my $timestr = "";
  if ($seconds >= 86400) {
	my $days = int($seconds / 86400);
	$seconds %= 86400;
	$timestr = "${days}d";
  }
  if ($seconds >= 3600) {
	my $hours = int($seconds / 3600);
	$seconds %= 3600;
	$timestr .= "${hours}h";
  }
  if ($seconds >= 60) {
	my $minutes = int($seconds / 60);
	$seconds %= 60;
	$timestr .= "${minutes}m";
  }
  $timestr .= "${seconds}s";
  return $timestr;
}

sub qsub_args {
  my ($self,@args) = @_;

  $self->{qsub_args} = join(" ",@args);
}

1;

