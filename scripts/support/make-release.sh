#!/bin/bash

# This script packages up an end-user version of Joshua for download.

set -u

version=$1

cd $JOSHUA
ant clean java documentation
[[ ! -d release ]] && mkdir release
ln -s $JOSHUA joshua-$version

# version without docs
tar czf release/joshua-$version.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-$version/{README.txt,INSTALL.txt,build.xml,logging.properties} \
    joshua-$version/bin \
    joshua-$version/lib/{*jar,eng_sm6.gr,README,LICENSES} \
    joshua-$version/scripts \
    joshua-$version/examples \
    joshua-$version/thrax/bin/thrax.jar \
    joshua-$version/tree_visualizer

# docs version
tar czf release/joshua-$version-with-docs.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-$version/{README.txt,INSTALL.txt,build.xml,logging.properties} \
    joshua-$version/bin \
    joshua-$version/lib/{*jar,eng_sm6.gr,README,LICENSES} \
    joshua-$version/doc \
    joshua-$version/scripts \
    joshua-$version/examples \
    joshua-$version/thrax/bin/thrax.jar \
    joshua-$version/tree_visualizer

rm -f joshua-$version
