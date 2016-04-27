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
(for file in lm lm.gz lm.berkeleylm lm.berkeleylm.gz; do
    echo the chat-rooms | $JOSHUA/bin/joshua-decoder -feature-function "LanguageModel -lm_type berkeleylm -lm_order 2 -lm_file $file" -v 0 -output-format %f 2> log
done) > output

# Compare
diff -u output output.gold > diff

if [ $? -eq 0 ]; then
  rm -f diff output log
  exit 0
else
  exit 1
fi
