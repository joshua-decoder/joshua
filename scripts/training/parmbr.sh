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
# Parallelizes Joshua's MBR rescoring with parallelize.pl.  Usage:
#
#   echo SENTNO | parmbr.sh NBEST_FILE CLASSPATH
#
# The script works by receiving a sentence number (SENTNO) on standard
# input and using it to select the nbest output from the NBEST_FILE,
# then calling the Joshua MBR class using the provided CLASSPATH.  It
# then computes the minimum risk solution and outputs it to standard
# output.

nbest_file=$1
classpath=$2

if ! test -e "$nbest_file"; then
	echo "parmbr.sh: no such nbest file '$nbest_file'"
	exit
fi

while read sentno; do

	grep "^$sentno " $nbest_file | java -cp $classpath -Xmx1700m -Xms1700m joshua.decoder.NbestMinRiskReranker false 1 2> /dev/null

done
