The interesting files in this folder are:

1) Aligning src-cand:
nbest.out.ZMERT.it6.25sen.trees
Align.config.25sen.txt
out_SrcCand.align.25sen

2) Extracting alternative translations:
GQ.config.unrest.25sen.txt
newQueries.unrest.25sen.txt
GQ.config.rest.25sen.txt
newQueries.rest.25sen.txt

3) Generating .csv input for Mechanical Turk:
mturk.rest.25sen.input.csv
mturk.rest.25sen.input.lng.csv
mturk.rest.25sen.input.shr.csv
template_hmert.htm
template_hmert_refhighlighted.htm

Here's a basic outline of what the process of creating queries looks like:

1) AlignCandidates aligns source sentences to candidates, creating a file like out_SrcCand.align.25sen.

2) GenerateQueries takes above file and creates a human-readable "query-info" file such as newQueries.unrest.25sen.txt.  The *.rest.* file is a "restricted" version, where, for example, long source substrings are not considered.  See last 6 lines of GQ.config.rest.25sen.txt.

3) GenerateCSV takes above file and creates input for Mechanical Turk, where each line corresponds to a single source subtree, and could have up to 10 translation alternatives (10 being provided at runtime).  Each line also includes the source and reference information.  The input is created as 2 files, one for longer source segments (>= 3 words) and one for shorter source segments (<= 2 words).


And here's a more detailed overview:

1) AlignCandidates takes as its input the decoder's output file, with the sentences represented as derivation trees, and deduces a word alignment from the source sentences to the candidates, based on information in the derivation tree, as well as alignment information from the training corpus.

Sample input: nbest.out.ZMERT.it6.25sen.trees -- candidates for the first 25 sentences of newstest2008_de-en, using weights from the end of a MERT run optimizing BLEU.

Sample output: out_SrcCand.align.25sen -- each candidate corresponds to 2 lines in this file, one for the basic phrasal alignments deduced from the derivation tree, and one for more detailed word alignments.  Phrasal alignments are not used from here on, and are only printed for us to stare at.

2) GenerateQueries takes as its input the src-cand alignment information from the previous phase, and the source parse trees, and creates a "query-info" file, where each line corresponds to a subtree in the parse tree and possible translation, and has the following format:

i j_k ||| candSubStr ||| occCount ||| candStr ||| startIndex

For illustration, examine lines 106-108 in newQueries.rest.25sen.txt.  This says that in source sentence #1, the word sequence indexed 5-5 had 3 possible translations, that occurred 117, 100, and 83 times, respectively (summing up to 300, the number of candidates).  For each alternative, candStr is the *full* sentence of the candidate where this alternative was observed (and startIndex tells you where in the candidate candSubStr starts).

Notice that a candSubStr can have a "gap" in it, signified by [Skip]'s.  See for example lines 264-267, which show that the first 2 words in sentence 3 had 4 different alternative translations across that sentence's 81 candidates.  Two of those alternatives have a gap in them.

3) GenerateCSV takes as its input the "human-readable" file from the previous phase, and creates a .csv file for Mechanical Turk, which also includes the source and reference sentences.  Each line (corresponding to a single HIT) corresponds to a single "i j_k", and could have up to 10 translation alternatives (10 is provided at runtime).

A sample output file is mturk.rest.25sen.input.lng.csv.  It is used in conjunction with template_hmert.htm to create the HITs.
