#!/bin/bash
echo TODO\(juri\): Makefile to memoize instead of compiling kenlm every time.  
 set -e
base="$(dirname "$0")"
output="$(readlink -f "$1")"
pushd "$base" >/dev/null
./compile.sh
g++ -I. -DNO_ICU -DNDEBUG -O3 $CXXFLAGS jni/wrap.cc -I$JAVA_HOME/include{,/linux} util/{bit_packing,ersatz_progress,exception,file_piece,murmur_hash,scoped,mmap}.o lm/{binary_format,config,lm_exception,model,read_arpa,search_hashed,search_trie,trie,virtual_interface,vocab}.o -fpic -shared -Wl,-soname,libken.so -o "$output"/libken.so -lz
popd >/dev/null
