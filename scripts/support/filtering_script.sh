#!/bin/tcsh
#$ -S /bin/tcsh

set input_grammar = $1
set corpus = $2
set output_grammar = $3

echo STARTED `date` >> /dev/stderr

gzip -cdf $input_grammar | java -Xmx2g -Dfile.encoding=utf8 -cp $JOSHUA/thrax/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter -v $corpus | $JOSHUA/scripts/training/remove-unary-abstract.pl | gzip -9 > $output_grammar

echo FINISHED `date` >> /dev/stderr

