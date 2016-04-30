#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
