#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum

rm -f test.nbest test.1best

java \
	-classpath "../bin"          \
	-Djava.library.path=../lib   \
	-Xmx500m -Xms500m            \
	-XX:MinHeapFreeRatio=10      \
	joshua.decoder.JoshuaDecoder \
	config.test                  \
	test.plf                     \
	test.nbest

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi


java \
	-classpath "../bin"        \
	joshua.util.ExtractTopCand \
	test.nbest             \
	test.1best

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


