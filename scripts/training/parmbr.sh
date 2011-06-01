#!/bin/bash

# Parallelizes Joshua's MBR rescoring with parallelize.pl.  Usage:
#
#   echo SENTNO | parmbr.sh NBEST_FILE CLASSPATH
#
# The script works by receiving a sentence number (SENTNO) on standard
# input and using it to select the nbest output from the NBEST_FILE,
# then calling the Joshua MBR class using the provided CLASSPATH.  It
# then computes the minimum risk solution and outputs it to standard
# output.

nbest_file=$1
classpath=$2

if ! test -e "$nbest_file"; then
	echo "parmbr.sh: no such nbest file '$nbest_file'"
	exit
fi

while read sentno; do

	grep "^$sentno " $nbest_file | java -cp $classpath -Xmx1700m -Xms1700m joshua.decoder.NbestMinRiskReranker false 1 2> /dev/null

done
