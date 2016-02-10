#!/bin/bash

set -u

export KENLM_MAX_ORDER=10
export CXXFLAGS+=" -O3 -fPIC -DHAVE_ZLIB"
export LDFLAGS+=" -lz"
export CXX=${CXX:-g++}

cd $JOSHUA/ext/kenlm
[[ ! -d build ]] && mkdir build
cd build
cmake .. -DKENLM_MAX_ORDER=$KENLM_MAX_ORDER -DCMAKE_BUILD_TYPE=Release
make -j2
cp bin/{query,lmplz,build_binary} $JOSHUA/bin

if [ "$(uname)" == Darwin ]; then
  SUFFIX=dylib
  RT=""
else
  RT=-lrt
  SUFFIX=so
fi

[[ ! -d "$JOSHUA/lib" ]] && mkdir "$JOSHUA/lib"
$CXX -std=gnu++11 -I. -DKENLM_MAX_ORDER=$KENLM_MAX_ORDER -I$JAVA_HOME/include -I$JOSHUA/ext/kenlm -I$JAVA_HOME/include/linux -I$JAVA_HOME/include/darwin $JOSHUA/jni/kenlm_wrap.cc lm/CMakeFiles/kenlm.dir/*.o util/CMakeFiles/kenlm_util.dir/*.o util/CMakeFiles/kenlm_util.dir/double-conversion/*.o -shared -o $JOSHUA/lib/libken.$SUFFIX $CXXFLAGS $LDFLAGS $RT
