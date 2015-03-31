#!/bin/bash

# This test case starts a server and then throws 10 threads at it to make sure threading is working.

$JOSHUA/bin/decoder -threads 4 -server-port 9010 -output-format "%i ||| %s" -mark-oovs true > server.log 2>&1 &
serverpid=$!
sleep 2

for num in $(seq 0 9); do
  echo -e "this\nthat\nthese\n\nthose\nmine\nhis\nyours\nhers" | nc localhost 9010 > output.$num 2> log.$num &
  pids[$num]=$!
done

for num in $(seq 0 9); do
  wait ${pids[$num]}
done

kill -15 $serverpid 2> /dev/null

paste output.* > output

diff -u output expected > diff

if [[ $? -eq 0 ]]; then
  rm -f server.log output output.* log.* diff
  exit 0
else
  exit 1
fi
