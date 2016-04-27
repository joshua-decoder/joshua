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
# Build a GHKM system. Moses is used to extract the GHKM grammar, which is then
# converted to Joshua's (similar) format automatically. Moses is also used for
# MIRA tuning.

# qsub arguments (if used on a cluster)
#$ -cwd
#$ -l num_proc=4,h_vmem=20g,h_rt=168:00:00
#$ -S /bin/bash
#$ -j y -o ghkm.log

. ~/.bashrc

set -u

#export MOSES=/path/to/moses/version/2.1.1

$JOSHUA/scripts/training/pipeline.pl \
    --rundir 1 \
    --readme "Building a baseline GHKM model" \
    --type ghkm \
    --source es \
    --target en \
    --corpus $JOSHUA/examples/data/corpus/asr/fisher_train \
    --corpus $JOSHUA/examples/data/corpus/asr/callhome_train \
    --tune $JOSHUA/examples/data/corpus/asr/fisher_dev \
    --test $JOSHUA/examples/data/corpus/asr/fisher_dev2 \
    --threads 4 \
    --joshua-mem 20g \
    --packer-mem 8g \
    --tuner mira \
    --maxlen 80 \
    --optimizer-runs 5
