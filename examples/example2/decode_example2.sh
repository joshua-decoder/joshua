#!/bin/sh

# The number of MB to give to Java's heap
# For this example 500 is minimum
# For 32-bit Java 2048 (or so) is the maximum
MEM=1g

case "$1" in
	javalm | kenlm | packed ) : ;;
	* )
		echo "Usage: $0 javalm"
		echo "       $0 kenlm"
		exit 1
	;;
esac

rm -f example2/example2.nbest example2/example2.1best \
      example2/example2.1best.sgm example2/example2.refs.sgm

cat example2/example2.src | \
	$JOSHUA/joshua-decoder -m $MEM -threads 2 -c example2/example2.config.$1 > \
	example2/example2.${1}.nbest

exitCode=$?
if [ $exitCode -ne 0 ]; then
	echo "Something went wrong with the parser: $exitCode"
	exit $exitCode
fi

java -cp $JOSHUA/bin -Dfile.encoding=utf8 -Xmx256m -Xms256m \
	joshua.util.JoshuaEval -cand example2/example2.${1}.nbest -format nbest -ref example2/example2.ref -rps 4 -m BLEU 4 closest

