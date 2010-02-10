# This Makefile is called by Ant. There's no need to use it directly.


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
SRILM_INC=-I$(SRILM_ROOT)/include

# Define where the installed library will go
SRI_LIB_DIR:=../../../../../../lib
SRI_LIB_INSTALLED:=$(SRI_LIB_DIR)/$(notdir $(SRI_LIB))

# Java headers
# TODO: What's the behaviour on Windows?
JAVA_INC=-I$(JAVA_HOME)/include/

ifneq ($(realpath $(JAVA_HOME)/include/linux),)
JAVA_INC_OS=-I$(JAVA_HOME)/include/linux 
endif



# Input files: srilm.h, srilm.c, and srilm_java.i

all: dummy clean libsrilm install

dummy:
	@echo "# $(64BIT_FLAG)"

libsrilm: $(SRI_LIB)
	@# This phony target is to give a consistent name for
	@# building the library in case people don't want to use
	@# 'all' (hence 'clean')

$(SRI_LIB): srilm.o srilm_java_wrap.o
	@# FIXME: is this really necessary?
#	@echo "# hi there $(64BIT_FLAG)"
	@# BUG: On OSX 10.4, g++ 4.0.1: "-fpic is not supported; -fPIC assumed"
	g++ $(64BIT_FLAG) -fpic $(SRI_LFLAG) $^ -loolm -ldstruct -lmisc -L$(SRILM_LIBS) -o $@

srilm_java_wrap.o: srilm_java_wrap.cxx
	@# BUG: On OSX 10.4, g++ 4.0.1: "-fpic is not supported; -fPIC assumed"
	g++ $(64BIT_FLAG) -c -fpic $< -I/usr/local/include/ $(SRILM_INC) $(JAVA_INC) $(JAVA_INC_OS)

srilm_java_wrap.cxx: srilm_java.i
	swig -c++ -java -package joshua.decoder.ff.lm.srilm -outdir . $<

srilm.o: srilm.c
	@# BUG: On OSX 10.4, g++ 4.0.1: "-fpic is not supported; -fPIC assumed"
	g++ $(64BIT_FLAG) -c -fpic $< -I/usr/local/include/ $(SRILM_INC)


clean:
	rm -rf srilm.o srilm*.java srilm_*_wrap.* SWIGTYPE*.java $(SRI_LIB)



install: $(SRI_LIB_INSTALLED)

$(SRI_LIB_INSTALLED): $(SRI_LIB) | $(SRI_LIB_DIR)
	cp $(SRI_LIB) $(SRI_LIB_DIR)

distclean: clean
	rm -f $(SRI_LIB_INSTALLED)

# First undefine all the rules for suffixes, then add ours in.
# (Actually, we don't have any generic rules. Maybe we should genericize)
.SUFFIXES:
#.SUFFIXES: .cxx .o .so .dylib .jnilib

# Marking these tarkets as phony ensures they are always run if
# asked to (even if someone's named a file after them).
.PHONY: all dummy libsrilm clean install distclean

