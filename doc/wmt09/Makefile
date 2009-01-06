SHELL = /bin/bash

LATEX=latex
DVIPS=dvips
PS2PDF=ps2pdf
BIBTEX=bibtex

LATEX_OPTS=-interaction=nonstopmode -halt-on-error
DVIPS_OPTS=-Ppdf
### A4size
###-t letter
PS2PDF_OPTS=-dPDFSETTINGS=/prepress -dSubsetFonts=true -dEmbedAllFonts=true
NUM=""

RERUN = "(There were undefined references|Rerun to get (cross-references|the bars) right)"
RERUNBIB = "No file.*\.bbl|Citation.*undefined"


.PHONY: all clean


all: ../bibliography.bib wmt09_system_description.pdf

%: %.pdf *.tex

%.pdf : %.ps
	$(PS2PDF) $(PS2PDF_OPTS) $<

#%.pdf : %.tex
#	pdflatex $<

%.ps: %.dvi
	$(DVIPS) $(DVIPS_OPTS) $<

%.dvi: %.tex $(wildcard *.tex) $(wildcard ../styles/*.sty) ../bibliography.bib
	$(LATEX) $(LATEX_OPTS) $<
	$(BIBTEX) $(patsubst %.tex,%,$<)
	$(LATEX) $(LATEX_OPTS) $<
	$(LATEX) $(LATEX_OPTS) $<
	rm -f $(patsubst %.tex,%,$<).log

%.bbl: %.tex ../bibliography.bib
	if [ -n %.aux ]; \
	then \
		$(LATEX) $(LATEX_OPTS) $<;\
	fi
	$(BIBTEX) $(patsubst %.tex,%,$<)

%.again: %.tex
	$(LATEX) $(LATEX_OPTS) $<

clean:
	+rm -fv *.{dvi,ps,pdf,aux,log,bbl,blg}
