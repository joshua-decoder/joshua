package joshua.decoder.ff.tm;

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunctionList;
import joshua.decoder.ff.tm.HieroGrammar.DiskHyperGraphFormatReader;
import joshua.decoder.ff.tm.HieroGrammar.HieroFormatReader;
import joshua.decoder.ff.tm.HieroGrammar.SamtFormatReader;

/**
 * This enum contains the various grammar/lexicon formats understood by Joshua.
 * Each grammar file format should be represented here, along with the ties to
 * the corresponding reader. This enum serves as a factory.
 * 
 * @author Juri Ganitkevitch
 * 
 */

public enum GrammarFormat {
	HIERO_DEFAULT {
		public GrammarReader<BilingualRule> createReader(
				String grammarFile, SymbolTable vocabulary,
				FeatureFunctionList features) {
			return new HieroFormatReader(grammarFile, vocabulary, features);
		}
		
		public String toString() {
			return "standard Hiero translation grammar format";
		}
	},
	
	JOSHUA_HYPERGRAPH {
		public GrammarReader<BilingualRule> createReader(
				String grammarFile, SymbolTable vocabulary,
				FeatureFunctionList features) {
			return new DiskHyperGraphFormatReader(grammarFile, vocabulary, features);
		}
		
		public String toString() {
			return "standard Hiero translation grammar format";
		}
	},
	
	SAMT_DEFAULT {
		public GrammarReader<BilingualRule> createReader(
				String grammarFile, SymbolTable vocabulary,
				FeatureFunctionList features) {
			return new SamtFormatReader(grammarFile, vocabulary, features);
		}
		
		public String toString() {
			return "standard SAMT translation grammar format";
		}
	};

	// TODO: back off to generic <R extends Rule> for non-bilingual formats?
	public abstract GrammarReader<BilingualRule> createReader(
			String grammarFile, SymbolTable vocabulary,
			FeatureFunctionList features);

	public abstract String toString();
	
	public static GrammarFormat parse(String keyword) {
		keyword = keyword.toLowerCase();

		if (keyword.equals("samt")) {
			return SAMT_DEFAULT;
		} else if (keyword.equals("hiero")) {
			return HIERO_DEFAULT;
		} else {
			// TODO: introduce a config-specific FormatException to throw here 
			return HIERO_DEFAULT;
		}
	}
}
