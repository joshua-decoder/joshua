#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2100 is maximum
MEM=500

case "$1" in
	javalm | srilm ) : ;;
	* )
		echo "Usage: $0 javalm"
		echo "       $0 srilm"
		exit 1
	;;
esac

rm -f example2.nbest example2.1best example2.1best.sgm example2.refs.sgm

java    -classpath "$CLASSPATH:../bin"  \
	-Djava.library.path=../lib       \
	-Xmx${MEM}m -Xms${MEM}m          \
	joshua.decoder.JoshuaDecoder     \
	example2.config.$1               \
	example2.src                     \
	example2.nbest

if [ $? -ne 0 ]; then
	echo "Something went wrong with the parser: $?"
	exit $?
fi

./get_1best_from_Nbest.pl example2.nbest example2.1best
./get_IBM_SGML_from_1best.pl example2

# Note: we do not distribute IBM's BLEU scorer because it is
# (C) Copyright IBM Corp. 2001 All Rights Reserved.
# The symlink in Subversion points to JHU's local copy on the CLSP grid
./bleu-1.04.pl -t example2.1best.sgm -r example2.refs.sgm -n 4 -ci
