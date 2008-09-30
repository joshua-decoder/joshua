
SRC        =src/
EXAMPLE    =example/example
JAVA_FLAGS =


.SUFFIXES:
.SUFFIXES: .java .class

.PHONY: all joshua test srilm_inter clean

all:
	@$(MAKE) clean
	@$(MAKE) joshua
	@$(MAKE) test

joshua:
	( cd $(SRC) && \
		find -X joshua/decoder/ -name '*.java' \
		| xargs javac $(JAVA_FLAGS) )

test:
	java -Xmx2000m -Xms2000m           \
		-classpath $(SRC)              \
		joshua.decoder.Decoder         \
		$(EXAMPLE).config.javalm       \
		$(EXAMPLE).test.in             \
		$(EXAMPLE).nbest.javalm.out    \
		2>&1 | tee $(EXAMPLE).nbest.javalm.err

srilm_inter:
	( cd $(SRC)joshua/decoder && make )

clean:
	find ./$(SRC) -name '*.class' -exec rm {} \;
