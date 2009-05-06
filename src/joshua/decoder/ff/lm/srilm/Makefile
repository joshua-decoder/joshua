
# Initialization
# Machine type as used by SRILM
# Sometimes, machine-type is wrong, so if there's only one library, use it
SRILM_LIBS = $(shell ls $(SRILM)/lib)
ifeq (1 , $(words $(shell ls $(SRILM)/lib)))
MTYPE := $(SRILM_LIBS)
else
MTYPE := $(shell $(SRILM)/sbin/machine-type)
endif

SRI_LFLAG = -shared
SRI_LIB = libsrilm.so

# Change to OS X-specific values

ifeq ($(MTYPE), macosx)
	SRI_LFLAG = -dynamiclib
	SRI_LIB = libsrilm.dylib
else
ifeq ($(MTYPE), macosx-m64)
	SRI_LFLAG = -dynamiclib
	SRI_LIB = libsrilm.dylib
	64BIT_FLAG=-m64
endif
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

all: dummy clean $(SRI_LIB) install

dummy:
	echo $(64BIT_FLAG)

$(SRI_LIB): srilm.o srilm_java_wrap.o
	echo hi there $(64BIT_FLAG)
	g++ $(64BIT_FLAG) -fpic $(SRI_LFLAG) $^ -loolm -ldstruct -lmisc -L$(SRILM_LIBS) -o $@

srilm_java_wrap.o: srilm_java_wrap.cxx
	g++ $(64BIT_FLAG) -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC) -I$(JAVA_INC) -I$(JAVA_INC_OS)

srilm_java_wrap.cxx: srilm_java.i
	swig -c++ -java -package joshua.decoder.ff.lm.srilm -outdir . $<


# General
srilm.o: srilm.c
	g++ $(64BIT_FLAG) -c -fpic $< -I/usr/local/include/ -I$(SRILM_INC)

clean:
	rm -rf srilm.o srilm*.java srilm_*_wrap.* SWIGTYPE*.java $(SRI_LIB)

install:
	cp $(SRI_LIB) ../../../../../../lib/


