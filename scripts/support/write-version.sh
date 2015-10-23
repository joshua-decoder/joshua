#!/bin/bash

set -u

version=$(git describe --abbrev=0 --dirty)

# Save the current version and commit to a file
echo "release version: $(git describe --abbrev=0)" > $JOSHUA/VERSION
echo "current commit: $(git describe --long --dirty)" >> $JOSHUA/VERSION

