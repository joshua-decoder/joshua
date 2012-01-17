#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum

rm -f test.nbest test.1best

cat test.plf | $JOSHUA/joshua-decoder -m 500m config.test > test.nbest

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi


java -classpath $JOSHUA/bin joshua.util.ExtractTopCand test.nbest test.1best

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi

diff test.nbest test.expected
exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "TEST FAILED. Decoder failed to generate expected n-best list!"
	exit $exitCode
fi


