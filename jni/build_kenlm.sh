#!/bin/bash

set -u

export CXXFLAGS+=" -DNDEBUG -O3 -fPIC -DHAVE_ZLIB"
export LDFLAGS+=" -lz"
export CXX=${CXX:-g++}

cd $JOSHUA/src/kenlm
cmake .
make
cp bin/{query,lmplz,build_binary} $JOSHUA/bin

if [ "$(uname)" == Darwin ]; then
  SUFFIX=dylib
  RT=""
else
  RT=-lrt
  SUFFIX=so
fi

$CXX -I. -DKENLM_MAX_ORDER=9 -I$JAVA_HOME/include -I$JOSHUA/src/kenlm -I$JAVA_HOME/include/linux -I$JAVA_HOME/include/darwin $JOSHUA/jni/kenlm_wrap.cc lm/CMakeFiles/kenlm.dir/*.o util/CMakeFiles/kenlm_util.dir/*.o util/CMakeFiles/kenlm_util.dir/double-conversion/*.o -shared -o $JOSHUA/lib/libken.$SUFFIX $CXXFLAGS $LDFLAGS $RT
