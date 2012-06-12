package LocalConfig;

use strict;
use warnings;

use base 'Exporter';
our @EXPORT = qw( qsub_args mert_memory environment_name );

use Net::Domain qw(hostname hostfqdn hostdomain domainname);

my $host = domainname;

# keys are: HOST_REGEXP, MERTMem, QSubQueue, QSubMemFlag, QSubExtraFlags
my $CCONFIG = {
  'LTICluster' => {
    'HOST_REGEXP' => qr/^cluster\d+\.lti\.cs\.cmu\.edu$/,
    'QSubMemFlag' => '-l h_vmem=',
    'QSubExtraFlags' => '-l walltime=0:45:00',
    #'QSubQueue' => '-q long',
  },
  'UMIACS' => {
    'HOST_REGEXP' => qr/^d.*\.umiacs\.umd\.edu$/,
    'QSubMemFlag' => '-l pmem=',
    'QSubQueue' => '-q batch',
    'QSubExtraFlags' => '-l walltime=144:00:00',
  },
  'CLSP' => {
    'HOST_REGEXP' => qr/\.clsp\.jhu\.edu$/,
    'QSubMemFlag' => '-l mem_free=',
    'MERTMem' => '9G',
  },
  'Valhalla' => {
    'HOST_REGEXP' => qr/^(thor|tyr)\.inf\.ed\.ac\.uk$/,
  },
  'Blacklight' => {
    'HOST_REGEXP' => qr/^(blacklight.psc.edu|bl1.psc.teragrid.org)$/,
    'QSubMemFlag' => '-l pmem=',
  },
  'HLTCOE' => {
    'HOST_REGEXP' => qr/(test1|test2|test3|test4|r\d+n\d+|hltcoe\.jhu\.edu)/,
    'QSubMemFlag' => '-l num_proc=1,h_rt=24:00:00,mem_free=16g,h_vmem=',
#    'QSubQueue' => '-q '
#    'QSubQueue' => '-q mem.q'
  },
  'LOCAL' => {
    'HOST_REGEXP' => qr/local\.net$/,
    'QSubMemFlag' => '',
  },
};

our $senvironment_name;
for my $config_key (keys %$CCONFIG) {
  my $re = $CCONFIG->{$config_key}->{'HOST_REGEXP'};
  die "Can't find HOST_REGEXP for $config_key" unless $re;
  if ($host =~ /$re/) {
    $senvironment_name = $config_key;
  }
}

die "NO ENVIRONMENT INFO FOR HOST: $host\nPLEASE EDIT LocalConfig.pm\n" unless $senvironment_name;

our %CONFIG = %{$CCONFIG->{$senvironment_name}};
print STDERR "**Environment: $senvironment_name\n";

sub environment_name {
  return $senvironment_name;
}

sub qsub_args {
  my $mem = shift @_;
  my $queue = shift @_;
  die "qsub_args requires a memory amount as a parameter, e.g. 4G" unless $mem;
  my $mf = $CONFIG{'QSubMemFlag'} or die "QSubMemFlag not set for $senvironment_name";
  my $cmd = "qsub -S /bin/bash ${mf}${mem}";
  if ($CONFIG{'QSubQueue'} and $queue) { 
    $cmd .= ' ' . $CONFIG{'QSubQueue'} . " $queue";
  }
  if ($CONFIG{'QSubExtraFlags'}) { $cmd .= ' ' . $CONFIG{'QSubExtraFlags'}; }
  return $cmd;
}

sub mert_memory {
  return ($CONFIG{'MERTMem'} || '2G');
};

1;
