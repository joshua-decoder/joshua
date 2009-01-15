
# Initialization
# Machine type as used by SRILM
MTYPE := $(shell $(SRILM)/sbin/machine-type)

SRI_LFLAG = -shared
SRI_LIB = libsrilm.so

# Change to OS X-specific values
ifeq ($(MTYPE), macosx)
	SRI_LFLAG = -dynamiclib
	SRI_LIB = libsrilm.dylib
endif

# SRILM includes and libraries

SRILM_ROOT=$(SRILM)
SRILM_LIBS=$(SRILM_ROOT)/lib/$(MTYPE)
SRILM_INC=$(SRILM_ROOT)/include

# Java headers
# TODO: What's the behaviour on Windows?

JAVA_INC=$(JAVA_HOME)/include/
JAVA_INC_OS=$(JAVA_HOME)/include/linux 


# Input files: srilm.h, srilm.c, and srilm_java.i

all: clean $(SRI_LIB) install


$(SRI_LIB): srilm.o srilm_java_wrap.o
	g++ -fpic $(SRI_LFLAG) $^ -loolm -ldstruct -lmisc -L$(SRILM_LIBS) -o $@

srilm_java_wrap.o: srilm_java_wrap.cxx
	g++ -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC) -I$(JAVA_INC) -I$(JAVA_INC_OS)

srilm_java_wrap.cxx: srilm_java.i
	swig -c++ -java -package joshua.decoder.ff.lm.srilm -outdir . $<


# General
srilm.o: srilm.c
	g++ -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC)

clean:
	rm -rf srilm.o srilm*.java srilm_*_wrap.* SWIGTYPE*.java $(SRI_LIB)

install:
	cp $(SRI_LIB) ../../../../../../lib/


