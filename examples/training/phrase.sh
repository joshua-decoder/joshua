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
# Build a standard, unlexicalized phrase-based model. This will be very fast. Moses
# is needed to learn the phrase table.

. ~/.bashrc
set -u

#export MOSES=/path/to/moses/version/2.1.1

$JOSHUA/bin/pipeline.pl \
    --rundir 4 \
    --readme "Baseline phrase model" \
    --type phrase \
    --source es \
    --target en \
    --corpus $JOSHUA/examples/data/corpus/asr/fisher_train \
    --corpus $JOSHUA/examples/data/corpus/asr/callhome_train \
    --tune $JOSHUA/examples/data/corpus/asr/fisher_dev \
    --test $JOSHUA/examples/data/corpus/asr/fisher_dev2 \
    --threads 8 \
    --tuner mert \
    --joshua-mem 5g \
    --packer-mem 8g \
    --optimizer-runs 5
