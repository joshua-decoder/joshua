
.PHONY: gizapp mkcls-v2 install clean

all: gizapp mkcls-v2

gizapp:
	@echo $(JOSHUA)
	$(MAKE) -C GIZA++-v2

mkcls-v2:
	@echo $(JOSHUA)
	$(MAKE) -C mkcls-v2

install: gizapp mkcls-v2
	@cp GIZA++-v2/GIZA++ GIZA++-v2/snt2cooc.out mkcls-v2/mkcls $(JOSHUA)/bin/

clean:
	$(MAKE) -C GIZA++-v2 clean
	$(MAKE) -C mkcls-v2 clean
	@rm -f $(JOSHUA)/bin/GIZA++ $(JOSHUA)/bin/mkcls $(JOSHUA)/bin/snt2cooc.out
