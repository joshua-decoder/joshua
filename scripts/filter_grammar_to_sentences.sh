#!/bin/bash

# Takes a grammar and a corpus and filters the grammar to each
# sentence in that corpus.  Filtered files are placed in a newly
# created directory filtered/ in the current directory.  Each file
# has the name grammar.filtered.SENTNO.gz.  We assume the input
# grammar is compressed with gzip.

# Usage: grammar=GRAMMAR.GZ corpus=CORPUS ./filter.sh

# Works by calling itself recursively.  When $sentno is not defined,
# it makes the filtered/ subdir and calls itself for each sentence
# of the corpus.

. ~/.bashrc
. $CACHEPIPE/bashrc

set -u

: ${rundir=$(pwd)}
: ${sentno=-1}
: ${corpus=tune.de.tok.lc}
: ${grammar=../grammar.filtered.gz}

cd $rundir

if ! test -e "$corpus"; then
	echo "* FATAL: can't find corpus '$corpus'"
	exit
fi

# if sentno is defined, then run the cachecmd to build the
# sentence-level grammar file
if test $sentno -gt -1; then

	let minus=sentno-1

	cd filtered

	# cache the filtering step
	tmpfile=.tmp.$sentno
	/home/hltcoe/mpost/bin/mid $sentno ../$corpus > $tmpfile
	cachecmd filter-$sentno "gzip -cd $grammar | $THRAX/scripts/filter_rules.sh 12 $tmpfile | gzip -9 > grammar.filtered.$minus.gz" $grammar grammar.filtered.$minus.gz
	rm -f $tmpfile

else

	# if sentno is not defined, create the filtered directory and
	# start all the qsub jobs
	[[ ! -d "filtered" ]] && mkdir filtered
	numlines=$(cat $corpus | wc -l)
	# for sentno in $(seq 1 $numlines); do
	for sentno in $(seq 1 $numlines); do
		qsub -cwd -l num_proc=2 -q cpu.q -v sentno=$sentno,corpus=$corpus ./filter.sh
	done

fi
