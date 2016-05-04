#!/usr/bin/env bash
#
# Runs the Stanford dependency parser on one or more files, printing out the trees and the
# dependency links

if [ ! $# -ge 1 ]; then
  echo Usage: `basename $0` 'file(s)'
  echo
  exit
fi

set -u

scriptdir=$STANFORD_PARSER

cd $(dirname $1)
filename=$(basename $1)

# http://nlp.stanford.edu/software/parser-faq.shtml#u
java -mx64g -cp "$scriptdir/*:" edu.stanford.nlp.parser.lexparser.LexicalizedParser \
  -sentences newline -tokenized \
  -outputFormat "oneline,typedDependencies" \
  -maxLength 141 \
  -nthreads 16 edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz $filename | tee raw_parse.$filename 2> log.raw_parse.$filename \
  | $JOSHUA/scripts/morph/raw_parse_to_oneline.py

#java -mx2g -cp "$scriptdir/*:" edu.stanford.nlp.parser.nndep.DependencyParser \
#  -model edu/stanford/nlp/models/parser/nndep/english_UD.gz \
#  -tokenized -textFile $1 -outFile $1.parsed

#Exception in thread "main" java.lang.RuntimeException: Error: output tree format matt not supported. Known formats are: [penn, oneline, rootSymbolOnly, words, wordsAndTags, dependencies, typedDependencies, typedDependenciesCollapsed, latexTree, xmlTree, collocations, semanticGraph, conllStyleDependencies, conll2007]

