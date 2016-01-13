#!/bin/bash

# This script packages up an end-user version of Joshua for download.

set -u

cd $JOSHUA
ant java version

if [[ ! -e VERSION ]]; then
  echo "* FATAL: can't find the version file!"
  exit
fi

version=$(grep ^release VERSION | awk '{print $NF}')
commit=$(grep ^current VERSION | awk '{print $NF}')
clean=$(echo $commit | cut -d- -f2)

if [[ $clean != "0" ]]; then
  version=$commit
fi

echo "Bundling up joshua-$version"

[[ ! -d release ]] && mkdir release
rm -f joshua-$version && ln -s $JOSHUA joshua-$version

wget -r http://joshua-decoder.org/

tar czf release/joshua-$version.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-$version/{README.md,VERSION,CHANGELOG,build.xml,logging.properties} \
    joshua-$version/src \
    joshua-$version/jni \
    joshua-$version/bin \
    joshua-$version/class \
    joshua-$version/lib/{*jar,eng_sm6.gr,hadoop-2.5.2.tar.gz,README,LICENSES} \
    joshua-$version/scripts \
    joshua-$version/test \
    joshua-$version/examples \
    joshua-$version/thrax/bin/thrax.jar \
    joshua-$version/joshua-decoder.org

ln -sf joshua-$version release/joshua-runtime-$version
tar czf release/joshua-runtime-$version.tgz \
    --exclude='*~' --exclude='#*' \
    joshua-runtime-$version/{README.md,VERSION,CHANGELOG,build.xml,logging.properties} \
    joshua-runtime-$version/src/{joshua,kenlm,berkeleylm} \
    joshua-runtime-$version/jni \
    joshua-runtime-$version/bin \
    joshua-runtime-$version/class \
    joshua-runtime-$version/lib/{ant*,jung*,junit*jar,README,LICENSES} \
    joshua-runtime-$version/scripts \
    joshua-runtime-$version/examples \
    joshua-runtime-$version/joshua-decoder.org

rm -f joshua-$version
rm -f VERSION
