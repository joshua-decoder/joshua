#!/bin/bash

# Runs all the test cases (scripts named test*.sh) beneath this directory, reporting success or
# failure.  Each test script should echo "PASSED" (and return 0) on success or "FAILED" (and return
# nonzero) on failure.
#
# Important!  Do not rename this script to match the pattern test*.sh, or it will execute
# recursively.

GREEN='\033[01;32m'
RED='\033[01;31;31m'
NONE='\033[00m'

tests=$(find . -name test*.sh | perl -pe 's/\n/ /g')
echo "TESTS: $tests"

pass=0
fail=0
for file in $tests; do
  if [[ ! -x $file ]]; then
    continue;
  fi
  dir=$(dirname $file)
  name=$(basename $file)
  echo -n "Running test '$name' in test/$dir..."
  pushd $dir > /dev/null
  bash $name
  if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}PASSED${NONE}"
    let pass=pass+1
  else
    echo -e "${RED}FAILED${NONE}"
    let fail=fail+1
  fi
  popd > /dev/null
done

echo -e "${GREEN}PASSED${NONE} $pass ${RED}FAILED${NONE} $fail"
