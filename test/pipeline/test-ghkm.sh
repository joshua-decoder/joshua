#!/bin/bash
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

# This script tests the Joshua pipeline, training on a 1000-sentence Urdu-English parallel corpus,
# and tuning and testing on 100-sentence test sets with four references.  It uses the Berkeley
# aligner for alignment to avoid the dependency on compiling GIZA.

rm -rf 2
$JOSHUA/scripts/training/pipeline.pl \
    --readme "testing GHKM extraction" \
    --rundir 2                 \
    --type ghkm                \
    --source ur                \
    --target en                \
    --corpus input/train       \
    --last-step GRAMMAR        \
    --aligner-mem 4g           \
    --aligner berkeley > pipeline-ghkm.log 2>&1

#diff -u 1/test/final-bleu final-bleu.gold

size=$(perl -e "print +(stat('2/grammar.gz'))[7]")
if [[ $size -ne 0 ]]; then
	exit 0
else
	exit 1
fi

