#!/bin/bash

# Runs all the test cases (scripts named test*.sh) beneath this directory, reporting success or
# failure.  Each test script should echo "PASSED" (and return 0) on success or "FAILED" (and return
# nonzero) on failure.
#
# Important!  Do not rename this script to match the pattern test*.sh, or it will execute
# recursively.

for file in $(find . -name test*.sh); do
  if [[ ! -x $file ]]; then
    continue;
  fi
  dir=$(dirname $file)
  echo -n "Running test in $dir..."
  pushd $dir > /dev/null
  bash test.sh
  popd > /dev/null
done
