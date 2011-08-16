
.PHONY: gizapp mkcls-v2

all: gizapp mkcls-v2 bindir

gizapp:
	$(MAKE) -C GIZA++-v2

mkcls-v2:
	$(MAKE) -C mkcls-v2

bindir:
	@cp GIZA++-v2/GIZA++ mkcls-v2/mkcls GIZA++-v2/snt2cooc.out $(JOSHUA)/bin/

clean:
	$(MAKE) -C GIZA++-v2 clean
	$(MAKE) -C mkcls-v2 clean
	@rm $(JOSHUA)/bin/{GIZA++-v2,mkcls-v2,sntcooc.out}
