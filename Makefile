

joshua:
	javac edu/jhu/*/*.java

srilm_inter: 
	( cd edu/jhu/ckyDecoder && make )

clean:
	rm edu/jhu/*/*.class


