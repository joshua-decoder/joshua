
MTYPE := $(shell $(SRILM)/sbin/machine-type)

SRI_LFLAG = -shared
SRI_LIB = libsrilm.so

ifeq ($(MTYPE), macosx)
	SRI_LFLAG = -dynamiclib
	SRI_LIB = libsrilm.dylib
endif

#change this
SRILM_ROOT=$(SRILM)
SRILM_LIBS=$(SRILM_ROOT)/lib/$(MTYPE)
SRILM_INC=$(SRILM_ROOT)/include

#change this
JAVA_INC=$(JAVA_HOME)/include/
JAVA_INC_OS=$(JAVA_HOME)/include/linux 

#### input file: srilm.h, srilm.c, and srilm_java.i

#java
java: clean $(SRI_LIB)

$(SRI_LIB): srilm.o srilm_java_wrap.o
	g++ -fpic $(SRI_LFLAG) $^ -loolm -ldstruct -lmisc -L$(SRILM_LIBS) -o $@

srilm_java_wrap.o: srilm_java_wrap.cxx
	g++ -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC) -I$(JAVA_INC) -I$(JAVA_INC_OS)


srilm_java_wrap.cxx: srilm_java.i
	swig -c++ -java -package joshua.decoder.ff.lm.srilm -outdir . $<



#general
srilm.o: srilm.c
	g++ -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC)

clean:
	rm -rf srilm.o srilm*.java srilm_*_wrap.* SWIGTYPE*.java $(SRI_LIB)
