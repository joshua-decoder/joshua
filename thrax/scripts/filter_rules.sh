#!/bin/bash

if (($# < 1))
then
    cat << END_USAGE
usage: filter_rules.sh [-v|-p|-f] <test set> [test set ...]
    -v  verbose mode
    -p  parallel compatibility: print blank lines, don't buffer output
    -f  fast mode: not as aggressive
END_USAGE
    exit 1
fi

java -Dfile.encoding=utf8 -cp $THRAX/bin/thrax.jar edu.jhu.thrax.util.TestSetFilter $*

