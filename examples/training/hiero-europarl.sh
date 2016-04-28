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
# This builds a model on Europarl (assuming you've downloaded it:
# http://statmt.org/wmt14/translation-task.html), tuning against WMT's
# newstest 2013, and testing against 2014. It also includes an extra LM,
# built (separately, using KenLM's tool available at $JOSHUA/bin/lmplz)
# on Gigaword

. ~/.bashrc
set -u

$JOSHUA/bin/pipeline.pl \
    --rundir 2 \
    --readme "Baseline Hiero model" \
    --type hiero \
    --source es \
    --target en \
    --corpus /path/to/europarl/europarl-v7.es-en
    --corpus /path/to/europarl/news-commentary-v7.es-en
    --tune /path/to/newstest/newstest2013
    --test /path/to/newstest/newstest2014
    --threads 8 \
    --tuner mert \
    --joshua-mem 20g \
    --packer-mem 16g \
    --lmfile /path/to/gigaword/lmfile \
    --optimizer-runs 5
