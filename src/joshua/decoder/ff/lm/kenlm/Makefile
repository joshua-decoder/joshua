all: kenlm

kenlm:
	./compile.sh
ifeq ($(shell uname -s),Darwin)
	g++ -I /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -I. -DNO_ICU -DNDEBUG -O3  jni/wrap.cc -I/System/Library/Frameworks/JavaVM.framework/Home/include{,/linux} util/{bit_packing,ersatz_progress,exception,file_piece,murmur_hash,scoped,mmap}.o lm/{binary_format,config,lm_exception,model,read_arpa,search_hashed,search_trie,trie,virtual_interface,vocab}.o -fpic -dynamiclib -Wl,-headerpad_max_install_names,-undefined,dynamic_lookup -o $(JOSHUA)/lib/libken.dylib -lz
else	
	g++ -I. -DNO_ICU -DNDEBUG -O3 $(CXXFLAGS) jni/wrap.cc -I$(JAVA_HOME)/include{,/linux} util/{bit_packing,ersatz_progress,exception,file_piece,murmur_hash,scoped,mmap}.o lm/{binary_format,config,lm_exception,model,read_arpa,search_hashed,search_trie,trie,virtual_interface,vocab}.o -fpic -shared -Wl,-soname,libken.so -o "$(libdir)"/libken.so -lz
endif
