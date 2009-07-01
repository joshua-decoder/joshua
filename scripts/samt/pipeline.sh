#!/bin/bash -e

if [ $# -ne 5 ]
then
  echo "Usage: pipeline.sh source target alignments target_parsed filter_set"
  exit 2
fi

MOSES="../moses/"
SAMT="../samt/"
TMP="../tmp"

#
# This is a terrible and hackish script. Use with care.
# Several assumptions and requirements exist.
#
# (1) Set the above variables to the installation directories of
#     the respective systems.
#
# (2) Add $JOSHUA/scripts/{samt,toolkit} to your $PATH, where
#     $JOSHUA is your Joshua install directory
#
# (3) The script expects you to have created the lexprob files as
#     follows:
#         - create a data.josh for the corpus (see PDF documentation)
#
#         - create a Joshua lexprob file for the corpus (see Joshua
#           dev list, message 707 and 708). Take care to run from the
#           Joshua installation dir for correct classpath. The output 
#           should be a file named data.lexprobs (set name in XML file).
#
#         - run lexprob2samt.py data.lexprobs to split into two 
#           SAMT-format lexprob files, data.lexprobs.samt.{sgt,tgs}
#
# (4) You'll need to change gzcat to zcat for this to run on Linux.
#
# (5) For the chunking to be of maximal use, if would be best if the 
#     sentence length distibution were uniform over the whole corpus.
#     I'll whip up a script for that sometime soon, if you deem it
#     useful.

chunki.py 45000 $1 $2 $3 $4

for C in chunk_*; do 
	($MOSES/scripts/training/phrase-extract/extract \
		$C/$2 $C/$1 \
		$C/$3 extract 8 --OnlyOutputSpanInfo > \
		$C/phrases.log &); 
done
wait

for C in chunk_*; do 
	(($SAMT/scripts/extractrules.pl \
		--PhrasePairFeedFile $C/phrases.log \
		--TargetParseTreeFile $C/$4 \
		-r $5 \
		--MaxSourceLength 12 \
		--LexicalWeightFile data.lexprobs.samt.sgt \
		--LexicalWeightFileReversed data.lexprobs.samt.tgs | \
		gzip > $C/extractrules.gz) >& $C/extractrules.log &); 
done
wait

gzcat chunk_*/extractrules.gz | $SAMT/scripts/sortsafe.sh -T $TMP | \
	$SAMT/bin/MergeRules 0 0 8 8 0 | gzip > mergedrules.gz

# TODO: filtering step to add lexical features.. ..are dropped by
#       MergeRules, need to re-add.

# throw away rules that do not have target side terminals

# TODO: work in progress here
# zgrep -v "[^#]*#[^a-z#]*"
	
