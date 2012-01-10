#!/bin/bash

if test -z $JOSHUA; then
    echo "You must set \$JOSHUA to the root of your Joshua installation."
    exit
fi

if test -z $3; then
    echo "Usage: tree-visualizer <source> <reference> <nbest-output>"
    echo "where"
    echo "  <source> is the source corpus, one sentence per line"
    echo "  <reference> is the reference corpus, one sentence per line"
    echo "  <nbest-output> is Joshua's n-best output"
    exit
fi

java -Xmx1g -jar tree_visualizer.jar $1 $2 $3
