package joshua.thrax;

import java.io.IOException;
import java.util.List;

import joshua.thrax.corpus.AlignedBitext;
import joshua.thrax.extractor.HierarchicalExtractor;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.suffix_array.SuffixArrayFactory;

import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.ff.tm.Rule;

public class HieroMain {

	private static final int CACHE_SIZE = 128;

	public static final int RULE_LENGTH = 7;

	public static void main(String [] argv)
	{
		if (argv.length < 3) {
			System.err.println("usage: HieroMain <source corpus> <target corpus> <alignment>");
			return;
		}

		try {

			Corpus src = SuffixArrayFactory.createCorpusArray(argv[0]);
			Corpus tgt = SuffixArrayFactory.createCorpusArray(argv[1], (Vocabulary) src.getVocabulary());
	
			Alignments al = SuffixArrayFactory.createAlignments(
			  argv[2],
		  	  SuffixArrayFactory.createSuffixArray(src, CACHE_SIZE),
		   	  SuffixArrayFactory.createSuffixArray(tgt, CACHE_SIZE));

			AlignedBitext bt = new AlignedBitext(src, tgt, al);

			HierarchicalExtractor ex = new HierarchicalExtractor(bt, RULE_LENGTH);

			List<Rule> rules = ex.getAllRules();

			for (Rule r : rules) {
				System.out.println(r.toString(src.getVocabulary() /*, src.getVocabulary(), tgt.getVocabulary() */));
			}

		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}

		return;
	}
}
