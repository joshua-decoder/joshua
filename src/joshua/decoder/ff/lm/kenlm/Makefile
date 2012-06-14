all: build_binary ngram_query install

CORE = util/bit_packing.o util/ersatz_progress.o util/exception.o util/file_piece.o util/murmur_hash.o util/scoped.o util/mmap.o lm/bhiksha.o lm/binary_format.o lm/config.o lm/lm_exception.o lm/model.o lm/quantize.o lm/read_arpa.o lm/search_hashed.o lm/search_trie.o lm/trie.o lm/virtual_interface.o lm/vocab.o
HEADERS= lm/bhiksha.hh lm/binary_format.hh lm/blank.hh lm/config.hh lm/enumerate_vocab.hh lm/facade.hh lm/lm_exception.hh lm/max_order.hh lm/model.hh lm/quantize.hh lm/read_arpa.hh lm/search_hashed.hh lm/search_trie.hh lm/sri.hh lm/trie.hh lm/virtual_interface.hh lm/vocab.hh lm/weights.hh lm/word_index.hh util/bit_packing.hh util/ersatz_progress.hh util/exception.hh util/file_piece.hh util/have.hh util/joint_sort.hh util/key_value_packing.hh util/mmap.hh util/murmur_hash.hh util/probing_hash_table.hh util/proxy_iterator.hh util/scoped.hh util/sorted_uniform.hh util/string_piece.hh

.cc.o: $(HEADERS)
	$(CC) -I. -O3 -c $(CFLAGS) $(CPPFLAGS) -fPIC -o $@ $<


ifeq ($(shell uname -s),Darwin)
libken.dylib: $(CORE) $(HEADERS)
	g++ -I /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -I. -DNO_ICU -DNDEBUG -O3 $(CXXFLAGS) jni/wrap.cc -I/System/Library/Frameworks/JavaVM.framework/Home/include -I/System/Library/Frameworks/JavaVM.framework/Home/include/linux util/bit_packing.o util/ersatz_progress.o util/exception.o util/file_piece.o util/murmur_hash.o util/scoped.o util/mmap.o lm/bhiksha.o lm/binary_format.o lm/config.o lm/lm_exception.o lm/model.o lm/quantize.o lm/read_arpa.o lm/search_hashed.o lm/search_trie.o lm/trie.o lm/virtual_interface.o lm/vocab.o -fpic -dynamiclib -Wl,-headerpad_max_install_names,-undefined,dynamic_lookup -o libken.dylib -lz -Wno-deprecated -pthread

install: libken.dylib
	cp -f libken.dylib $(JOSHUA)/lib/
else
libken.so: $(CORE) $(HEADERS) jni/wrap.cc
	g++ -I. -DNO_ICU -DNDEBUG -O3 $(CXXFLAGS) jni/wrap.cc -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux util/bit_packing.o util/ersatz_progress.o util/exception.o util/file_piece.o util/murmur_hash.o util/scoped.o util/mmap.o lm/bhiksha.o lm/binary_format.o lm/config.o lm/lm_exception.o lm/model.o lm/quantize.o lm/read_arpa.o lm/search_hashed.o lm/search_trie.o lm/trie.o lm/virtual_interface.o lm/vocab.o -fpic -shared -Wl,-soname,libken.so -o libken.so -lz -Wno-deprecated -pthread

install: libken.so
	cp -f libken.so $(libdir)/libken.so
endif

build_binary: $(CORE) $(HEADERS) lm/build_binary.cc
	g++ -I. -O3 $(CXXFLAGS) $(CORE) lm/build_binary.cc -o build_binary -lz
ngram_query: $(CORE) $(HEADERS) lm/ngram_query.cc
	g++ -I. -O3 $(CXXFLAGS) $(CORE) lm/ngram_query.cc -o ngram_query -lz

