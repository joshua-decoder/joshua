#!/bin/env python
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

import os
import sys
import argparse
import subprocess

parser = argparse.ArgumentParser(description='Compile an ARPA LM file into the BerkeleyLM binary format.')
parser.add_argument('-m', dest='mem', default='4g', help='How much memory to allocate for Java')
parser.add_argument('arpa_file', help='The ARPA file to compile')
parser.add_argument('output_file', help='The file to write to')
args = parser.parse_args()

cmd = "java -cp %s/ext/berkeleylm/jar/berkeleylm.jar -server -mx%s edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa %s %s" % (os.environ.get('JOSHUA'), args.mem, args.arpa_file, args.output_file)
print(cmd)
subprocess.call(cmd, shell=True)
