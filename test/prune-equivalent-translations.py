#!/usr/bin/env python

# A given source side can have multiple target sides, and these pairs
# can have the same scores.  This results in nondeterminism in the
# output, since any of the equally-scored derivations might be
# selected based on low-level architectural and library details.  This
# in turn makes testing difficult, because the results seem to fail.
# This script removes all but one of equivalent target sides.

import sys

skipped = 0
lastsource = ''
lastscores = []
for line in sys.stdin:
    lhs, source, target, scores = line.rstrip().split(' ||| ')
    if source == lastsource:
        if scores in lastscores:
            skipped += 1
#             sys.stderr.write('SKIPPING(%s)\n' % (line.rstrip()))
            continue

        lastscores.append(scores)

    else:
        lastsource = source
        lastscores = [scores]

    print line,

sys.stderr.write('skipped %d ambiguous translations\n' % (skipped))
