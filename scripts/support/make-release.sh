#!/bin/bash

# This script packages up an end-user version of Joshua for download.

set -u

version=$1

cd $JOSHUA
ant clean java
[[ ! -d release ]] && mkdir release
rm -f joshua-$version && ln -s $JOSHUA joshua-$version

wget -qr joshua-decoder.org

tar czf release/joshua-$version.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-$version/{README,build.xml,logging.properties} \
    joshua-$version/src \
    joshua-$version/bin \
    joshua-$version/lib/{*jar,eng_sm6.gr,hadoop-0.20.2.tar.gz,README,LICENSES} \
    joshua-$version/scripts \
    joshua-$version/data \
    joshua-$version/test \
    joshua-$version/examples \
    joshua-$version/thrax/bin/thrax.jar \
		joshua-$version/joshua-decoder.org \
    joshua-$version/joshua-decoder

rm -f joshua-$version
