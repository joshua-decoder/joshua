#!/bin/sh

# This test case should start a server, translate with it, and then kill the server.  Unfortunately,
# I can't get it to kill the server properly.  It kills the parent process, but Joshua is launched
# from a shell script, and so it doesn't kill the actual sub-process.

$JOSHUA/bin/decoder -server-port 9010 -output-format "%i ||| %s" > server.log 2>&1 &
serverpid=$!
sleep 2
ps ax | grep java

echo "this\nthat\nthese\n\nthose" | nc localhost 9010 > output 2> log

echo "killing pid $serverpid"
kill -15 $serverpid

diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
  echo PASSED
  rm -f server.log output log diff
  exit 0
else
  echo FAILED
  tail diff
  exit $?
fi


