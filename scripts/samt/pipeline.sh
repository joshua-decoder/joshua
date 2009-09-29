#!/bin/bash -e

if [ $# -ne 5 ]
then
  echo "Usage: pipeline.sh source target alignments target_parsed filter_set"
  exit 2
fi

MOSES="/home/hltcoe/ccallison/Moses/trunk/"
SAMT="/home/hltcoe/ccallison/SAMT/"
TMP="/tmp"

export MALLOC_CHECK_=0

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

HOLD_FOR=""

for C in chunk_*; do 
	JOB_ID=`qsub -S /bin/bash -V -N samt.phrase_extract.${C} \
		-h -b y \
		"($MOSES/scripts/training/phrase-extract/extract \
		$C/$2 $C/$1 \
		$C/$3 extract 8 --OnlyOutputSpanInfo > \
		$C/phrases.log )" | \
		sed -e "s/Your job \([0-9]*\).* has been submitted/\1/g"`;
	
	HOLD_FOR="${HOLD_FOR},"`qsub -S /bin/bash -V \
		-N samt.rule_extract.${C} \
		-h -b y -hold_jid ${JOB_ID} \
		"(($SAMT/scripts/extractrules.pl \
		--PhrasePairFeedFile $C/phrases.log \
		--TargetParseTreeFile $C/$4 \
		-r $5 \
		--MaxSourceLength 12 \
		--LexicalWeightFile data.lexprobs.samt.sgt \
		--LexicalWeightFileReversed data.lexprobs.samt.tgs | \
		gzip > $C/extractrules.gz) >& $C/extractrules.log )" | \
		sed -e "s/Your job \([0-9]*\).* has been submitted/\1/g"`;
done

HOLD_FOR=`echo ${HOLD_FOR} | sed -e "s/.\(.*\)/\1/g"`

JOB_ID=`qsub -S /bin/bash -V -N samt.rules_merge \
		-h -b y -hold_jid ${HOLD_FOR} \
		"zcat chunk_*/extractrules.gz | $SAMT/scripts/sortsafe.sh -T $TMP | \
		$SAMT/myoptions.coe/MergeRules 0 0 8 8 0 | gzip > mergedrules.gz" | \
		sed -e "s/Your job \([0-9]*\).* has been submitted/\1/g"`;

JOB_ID=`qsub -S /bin/bash -V -N samt.rules_merge \
		-h -b y -hold_jid ${JOB_ID} \
		"((zcat mergedrules.gz | \
		$SAMT/scripts/filterrules.pl --cachesize 4000 \
		--PhrasalFeatureCount 0 \
		--LexicalWeightFile data.lexprobs.giza.sgt \
		--LexicalWeightFileReversed data.lexprobs.giza.tgs \
		--MinOccurrenceCountLexicalrules 0 --MinOccurrenceCountNonlexicalrules 0 \
		--noUsePerlHashForRules | \
		gzip > filteredrules.gz ) >& filteredrules.log)" | \
		sed -e "s/Your job \([0-9]*\).* has been submitted/\1/g"`;

# throw away rules that do not have target side terminals
#zgrep -v "^\([^#]* \)*[^ @#][^ @#]*[^#]*#\(@[0-9][ ]*\)*#" filteredrules.gz | \
#	grep -v "#1 [0-9]" | gzip > filteredrules.clean.gz
		
#zgrep "#1 [0-9]" filteredrules.gz | grep -v COUNT | \
#	sed -e "s/@_S/@GOAL/g" | \
#	awk '{ print $0; gsub(/ @2/, ""); gsub(/^@GOAL /, ""); print; }' | \
#	gzip > samt.original.glue.gz

