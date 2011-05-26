#!/bin/bash
set -e

for i in util/{bit_packing,ersatz_progress,exception,file_piece,murmur_hash,scoped,mmap} lm/{binary_format,config,lm_exception,model,read_arpa,search_hashed,search_trie,trie,virtual_interface,vocab}; do
  g++ -I. -O3 $CXXFLAGS -fpic -c $i.cc -o $i.o
done
g++ -I. -O3 $CXXFLAGS lm/build_binary.cc {lm,util}/*.o -lz -o build_binary
g++ -I. -O3 $CXXFLAGS lm/ngram_query.cc {lm,util}/*.o -lz -o query
