all: build_binary ngram_query install

CC = g++

# The max order for the LM, this was increased to incorporate Class LM (9grams)
MAX_ORDER = 9

# Set to the location of your boost libraries (only set if not in the environment)
BOOST_ROOT ?= /opt/local

# Should be set to blank or "-mt", depending on whether you have the boost multithreaded
# libraries installed
#BOOST_MT = -mt

CORE = lm/bhiksha.o lm/binary_format.o lm/config.o lm/lm_exception.o lm/model.o lm/quantize.o lm/read_arpa.o lm/search_hashed.o lm/search_trie.o lm/sizes.o lm/trie.o lm/trie_sort.o lm/value_build.o lm/virtual_interface.o lm/vocab.o util/bit_packing.o util/ersatz_progress.o util/exception.o util/file.o util/file_piece.o util/mmap.o util/murmur_hash.o util/pool.o util/read_compressed.o util/scoped.o util/usage.o util/double-conversion/bignum.o util/double-conversion/bignum-dtoa.o util/double-conversion/cached-powers.o util/double-conversion/diy-fp.o util/double-conversion/double-conversion.o util/double-conversion/fast-dtoa.o util/double-conversion/fixed-dtoa.o util/double-conversion/strtod.o util/parallel_read.o

HEADERS= lm/bhiksha.hh lm/binary_format.hh lm/blank.hh lm/builder/adjust_counts.hh lm/builder/corpus_count.hh lm/builder/discount.hh lm/builder/header_info.hh lm/builder/initial_probabilities.hh lm/builder/interpolate.hh lm/builder/joint_order.hh lm/builder/multi_stream.hh lm/builder/ngram.hh lm/builder/ngram_stream.hh lm/builder/pipeline.hh lm/builder/print.hh lm/builder/sort.hh lm/config.hh lm/enumerate_vocab.hh lm/facade.hh lm/left.hh lm/lm_exception.hh lm/max_order.hh lm/model.hh lm/model_type.hh lm/ngram_query.hh lm/partial.hh lm/quantize.hh lm/read_arpa.hh lm/return.hh lm/search_hashed.hh lm/search_trie.hh lm/sizes.hh lm/state.hh lm/trie.hh lm/trie_sort.hh lm/value_build.hh lm/value.hh lm/virtual_interface.hh lm/vocab.hh lm/weights.hh lm/word_index.hh util/bit_packing.hh util/ersatz_progress.hh util/exception.hh util/fake_ofstream.hh util/file.hh util/file_piece.hh util/getopt.hh util/have.hh util/joint_sort.hh util/mmap.hh util/multi_intersection.hh util/murmur_hash.hh util/pcqueue.hh util/pool.hh util/probing_hash_table.hh util/proxy_iterator.hh util/read_compressed.hh util/scoped.hh util/sized_iterator.hh util/sorted_uniform.hh util/stream/block.hh util/stream/chain.hh util/stream/config.hh util/stream/io.hh util/stream/line_input.hh util/stream/multi_progress.hh util/stream/sort.hh util/stream/stream.hh util/stream/timer.hh util/string_piece_hash.hh util/string_piece.hh util/thread_pool.hh util/tokenize_piece.hh util/usage.hh util/parallel_read.hh

CPPFLAGS = $(CXXFLAGS) -I. -I$(BOOST_ROOT)/include -O3 -DKENLM_MAX_ORDER=$(MAX_ORDER) -DHAVE_ZLIB -DNDEBUG

.cc.o: $(HEADERS)
	$(CC) -c $(CPPFLAGS) -fPIC -o $@ $<

ifeq ($(shell uname -s),Linux)
RT=-lrt
endif

#query-related executables
EXES=build_binary ngram_query
build_binary: $(CORE) $(HEADERS) lm/build_binary_main.cc
	$(CC) $(CPPFLAGS) $(CORE) lm/build_binary_main.cc -o build_binary -lz $(RT)
ngram_query: $(CORE) $(HEADERS) lm/query_main.cc
	$(CC) $(CPPFLAGS) $(CORE) lm/query_main.cc -o ngram_query -lz $(RT)

INSTALL=build_binary ngram_query

#lmplz
SHELL=bash
ifeq (,$(wildcard $(BOOST_ROOT)/lib/libboost_thread.a))
  $(error BOOST_ROOT ($(BOOST_ROOT)) does not point to a Boost installation, quitting...)
endif
ifeq ($(shell $(CC) -L$(BOOST_ROOT)/lib -lboost_program_options$(BOOST_MT) -lboost_thread$(BOOST_MT) -x c++ - <<<'int main() {}' -o dummy && rm dummy && echo Boost),Boost)
  $(info Detected Boost)
LMPLZ=lm/builder/adjust_counts.o lm/builder/corpus_count.o lm/builder/initial_probabilities.o lm/builder/interpolate.o lm/builder/lmplz_main.o lm/builder/pipeline.o lm/builder/print.o lm/builder/output.o util/stream/chain.o util/stream/io.o util/stream/line_input.o util/stream/multi_progress.o
lmplz: $(CORE) $(HEADERS) $(LMPLZ)
	$(CC) $(CPPFLAGS) $(CORE) $(LMPLZ) -o lmplz -lz -L$(BOOST_ROOT)/lib -lboost_program_options$(BOOST_MT) -lboost_thread$(BOOST_MT) -lboost_system$(BOOST_MT) $(RT) -pthread
EXES+=lmplz 
else
  echo "lmplz (used for building language models) failed to build. Is Boost installed?"
  echo "If yes, maybe you have the multithreaded versions installed. Edit"
  echo "\$JOSHUA/src/joshua/decoder/ff/lm/kenlm/Makefile and uncomment the BOOST_MT line,"
  echo "then rerun 'ant kenlm'"
endif

#jni object
ifeq ($(shell uname -s),Darwin)
libken.dylib: $(CORE) $(HEADERS) jni/wrap.cc
	$(CC) -I /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -DNO_ICU $(CPPFLAGS) jni/wrap.cc -I/System/Library/Frameworks/JavaVM.framework/Home/include -I/System/Library/Frameworks/JavaVM.framework/Home/include/linux $(CORE) -fpic -dynamiclib -Wl,-headerpad_max_install_names,-undefined,dynamic_lookup -o libken.dylib -lz -Wno-deprecated -pthread

install: libken.dylib $(EXES)
	cp -f libken.dylib $(JOSHUA)/lib/
	cp -f $(EXES) $(JOSHUA)/bin/
else
libken.so: $(CORE) $(HEADERS) jni/wrap.cc
	$(CC) -I. -DNO_ICU $(CPPFLAGS) jni/wrap.cc -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux $(CORE) -fpic -shared -Wl,-soname,libken.so -o libken.so -lz -Wno-deprecated -pthread $(RT)

install: libken.so $(EXES)
	cp -f libken.so $(JOSHUA)/lib/libken.so
	cp -f $(EXES) $(JOSHUA)/bin/
endif

.PHONY: clean

clean:
	find . -name '*.o' -exec rm -f {} \;
	rm -f libken.dylib $(EXES)
