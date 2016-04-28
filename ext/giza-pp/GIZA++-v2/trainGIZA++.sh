#! /bin/csh
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
if( $# != 3 ) then

 echo Usage: trainGIZA++.sh vcb1 vcb2 snt
 echo " "
 echo Performs a training of word classes and a standard GIZA training.

else

    snt2plain.out $1 $2 $3 PLAIN

    mkcls -m2 -pPLAIN1.txt -c50 -V$1.classes opt >& mkcls1.log
    rm PLAIN1.txt
    mkcls -m2 -pPLAIN2.txt -c50 -V$2.classes opt >& mkcls2.log
    rm PLAIN2.txt
    GIZA++ -S $1 -T $2 -C $3 -p0 0.98 -o GIZA++ >& GIZA++.log

endif
