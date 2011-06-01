#!/bin/bash

# Parallelizes Joshua's MBR rescoring with parallelize.pl.  Usage:
#
#   echo SENTNO | parmbr.sh NBEST_FILE
#
# The script works by receiving a sentence number (SENTNO) on standard
# input and using it to select the nbest output from the NBEST_FILE.
# It then computes the minimum risk solution and outputs it to
# standard output.

nbest_file=$1

while read sentno; do
# if test -z $sentno; then
# 	lastsentindex=$(tail -n 1 $nbest_file | awk '{print $1}')

# 	for sentno in $(seq 0 $lastsentindex); do
# 		$SCRIPTDIR/parmbr.sh $nbest_file $sentno
# 	done
# else
	grep "^$sentno " $nbest_file | java -cp $JOSHUA/bin/ -Xmx1700m -Xms1700m joshua.decoder.NbestMinRiskReranker false 1
fi

$JOSHUA/scripts/training/parallelize/parallelize.pl -j 50 --
  $cachepipe->cmd("test-onebest-mbr", "java -cp $JOSHUA/bin -Xmx1700m -Xms1700m joshua.decoder.NbestMinRiskReranker test/test.output.nbest.noOOV test/test.output.1best false 1",
				  "test/test.output.nbest.noOOV", 
				  "test/test.output.1best");

