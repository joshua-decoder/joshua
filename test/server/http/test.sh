#!/bin/bash

# This test case starts a server and then throws 10 threads at it to make sure threading is working.

$JOSHUA/bin/decoder -threads 4 -server-port 9010 -server-type http -mark-oovs true > server.log 2>&1 &
serverpid=$!
sleep 1

curl -s http://localhost:9010/?q=I%20love%20it%20when%20I%20get%20the%20house%20clean%20before%20the%20weekend > output

kill -15 $serverpid 2> /dev/null

diff -u output expected > diff

if [[ $? -eq 0 ]]; then
  rm -f server.log output log diff
  exit 0
else
  exit 1
fi
