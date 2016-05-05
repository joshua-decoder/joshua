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
set -u

# Ensures that the decoder trims inputs when and only when it should

(
echo as kingfishers draw fire | joshua -maxlen 2
echo dragonflies draw flame | joshua -maxlen 1 -lattice-decoding
echo "(((as tumbled over rim in roundy wells stones ring" | joshua -maxlen 8
echo "(((like each tucked string tells" | joshua -maxlen 3 -lattice-decoding
) > output 2> log

diff -u output output.gold > diff

if [ $? -eq 0 ]; then
    rm -f log output diff
    exit 0
else
    exit 1
fi
