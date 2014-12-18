#!/bin/bash

# Wrapper around the grammar filter

java -Xmx4g -Dfile.encoding=utf8 -cp $JOSHUA/class joshua.tools.TestSetFilter "$@"
