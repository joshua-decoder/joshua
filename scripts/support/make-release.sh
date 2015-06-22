#!/bin/bash

# This script packages up an end-user version of Joshua for download.

set -u

cd $JOSHUA
ant clean java

version=$(cat $JOSHUA/VERSION | grep ^current | awk '{print $NF}')

[[ ! -d release ]] && mkdir release
rm -f joshua-$version && ln -s $JOSHUA joshua-$version

wget -r http://joshua-decoder.org/

ant version

tar czf release/joshua-$version.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-$version/{README.md,VERSION,CHANGELOG,build.xml,logging.properties} \
    joshua-$version/src \
    joshua-$version/bin \
    joshua-$version/class \
    joshua-$version/lib/{*jar,eng_sm6.gr,hadoop-0.20.2.tar.gz,README,LICENSES} \
    joshua-$version/scripts \
    joshua-$version/test \
    joshua-$version/examples \
    joshua-$version/thrax/bin/thrax.jar \
    joshua-$version/thrax/scripts \
    joshua-$version/joshua-decoder.org

rm -f joshua-$version
rm -f VERSION
