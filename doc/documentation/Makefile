# Makefile to compile the Joshua documentation

SHELL=/bin/bash
PAPER=joshua

BIBTEX=bibtex
LATEX=pdflatex

RERUN = "(There were undefined references|Rerun to get (cross-references|the bars) right)"
RERUNBIB = "No file.*\.bbl|Citation.*undefined"

.PHONY: all clean html



all: $(PAPER).pdf $(PAPER).bbl html

%.pdf: %.tex $(wildcard *.tex) $(wildcard styles/*.sty) 
	$(LATEX) $(LATEX_OPTS) $<
	$(BIBTEX) $(patsubst %.tex,%,$<)
	$(LATEX) $(LATEX_OPTS) $<
	$(LATEX) $(LATEX_OPTS) $<

%.bbl: %.tex ../../bibliography/bibliography.bib
	if [ -n %.aux ]; \
	then \
		$(LATEX) $(LATEX_OPTS) $<;\
	fi
	$(BIBTEX) $(patsubst %.tex,%,$<)

html: joshua.tex
	latex2html --local_icons joshua.tex

clean:
	+rm -fv *.{dvi,ps,aux,log,bbl,blg} $(PAPER).pdf
