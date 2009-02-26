
%/index.html %: %.xml *.xml
	xmlto -o $* html $<

%.html: %.xml *.xml
	xmlto xhtml-nochunks $<

%.pdf: %.xml *.xml
	dblatex $<

clean:
	rm *.pdf *.html */*.html