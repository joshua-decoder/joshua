
SRC           =src/
EXAMPLE       =example2/example2
EXAMPLE_SUFFIX=src
JAVA_FLAGS    =


.SUFFIXES:
.SUFFIXES: .java .class

.PHONY: all joshua test srilm_inter clean

all:
	@$(MAKE) clean
	@$(MAKE) joshua
	@$(MAKE) test EXAMPLE='example/example'   EXAMPLE_SUFFIX='test.in'
	@$(MAKE) test EXAMPLE='example2/example2' EXAMPLE_SUFFIX='src'

joshua:
	( cd $(SRC) && \
		find -X joshua/decoder/ -name '*.java' \
		| xargs javac $(JAVA_FLAGS) )

test:
	java -Xmx2000m -Xms2000m           \
		-classpath $(SRC)              \
		joshua.decoder.Decoder         \
		$(EXAMPLE).config.javalm       \
		$(EXAMPLE).$(EXAMPLE_SUFFIX)   \
		$(EXAMPLE).nbest.javalm.out    \
		2>&1 | tee $(EXAMPLE).nbest.javalm.err

srilm_inter:
	( cd $(SRC)joshua/decoder && make )

clean:
	find ./$(SRC) -name '*.class' -exec rm {} \;
	rm -f                                  \
		example/example.nbest.javalm.out   \
		example/example.nbest.javalm.err   \
		example2/example2.nbest.javalm.out \
		example2/example2.nbest.javalm.err 
