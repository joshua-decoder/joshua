#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum
MEM=1000

case "$1" in
	javalm | kenlm ) : ;;
	* )
		echo "Usage: $0 javalm"
		echo "       $0 kenlm"
		exit 1
	;;
esac

rm -f example2.nbest example2.1best example2.1best.sgm example2.refs.sgm

java \
	-classpath "../bin"          \
	-Djava.library.path=../lib   \
	-Dfile.encoding=utf8         \
	-Djava.util.logging.config.file=${JOSHUA}/logging.properties \
	-Xmx${MEM}m -Xms${MEM}m      \
	-XX:MinHeapFreeRatio=10      \
	joshua.decoder.JoshuaDecoder \
	example2/example2.config.$1  \
	example2/example2.src        \
	example2/example2.nbest

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi


java \
	-classpath "../bin"        \
	joshua.util.ExtractTopCand \
	example2/example2.nbest             \
	example2/example2.1best

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi

# TODO: we should run our BLEU scoring script

#./get_IBM_SGML_from_1best.pl example2
#./bleu-1.04.pl -t example2.1best.sgm -r example2.refs.sgm -n 4 -ci
