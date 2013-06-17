#!/bin/bash

# Runs all the test cases (scripts named test*.sh) beneath this directory, reporting success or
# failure.  Each test script should echo "PASSED" (and return 0) on success or "FAILED" (and return
# nonzero) on failure.
#
# Important!  Do not rename this script to match the pattern test*.sh, or it will execute
# recursively.


tests=$(find . -name test*.sh | perl -pe 's/\n/ /g')
echo "TESTS: $tests"

for file in $tests; do
  if [[ ! -x $file ]]; then
    continue;
  fi
  dir=$(dirname $file)
  name=$(basename $file)
  echo -n "Running test '$name' in $dir..."
  pushd $dir > /dev/null
  bash $name
  popd > /dev/null
done
