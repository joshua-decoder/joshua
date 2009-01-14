
SRC           = src
EXAMPLE       = example2/example2
EXAMPLE_SUFFIX= src
JAVA_FLAGS    =


.PHONY: all joshua test srilm_inter srilm_clean clean

all:
	@$(MAKE) clean
	@$(MAKE) srilm_inter
	@$(MAKE) joshua
#
#	deactivated for now
#
#	@$(MAKE) test EXAMPLE='example/example'   EXAMPLE_SUFFIX='test.in'
#	@$(MAKE) test EXAMPLE='example2/example2' EXAMPLE_SUFFIX='src'

rules:
	@$(MAKE) -f Makefile.lexprobs

joshua:
	ant compile

test:	joshua
	ant test	

srilm_inter:
	$(MAKE) -C $(SRC)/joshua/decoder/ff/lm/srilm

srilm_clean:
	$(MAKE) -C $(SRC)/joshua/decoder/ff/lm/srilm clean

clean: srilm_clean
	ant clean
	@$(MAKE) -f Makefile.lexprobs clean
	rm -f                                  \
		example/example.nbest.javalm.out   \
		example/example.nbest.javalm.err   \
		example2/example2.nbest.javalm.out \
		example2/example2.nbest.javalm.err 
