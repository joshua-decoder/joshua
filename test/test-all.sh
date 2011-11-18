#!/bin/bash

# Runs all the test cases (scripts named test.sh) beneath this
# directory, reporting success or failure.

for file in $(find . -name test.sh); do
	dir=$(dirname $file)

	echo -n "Running test in $dir..."
	pushd $dir > /dev/null
	bash test.sh

	# fail on failure
	if [[ $? -ne 0 ]]; then
		exit 1
	fi

	popd > /dev/null
done
