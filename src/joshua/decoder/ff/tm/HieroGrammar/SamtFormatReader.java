package joshua.decoder.ff.tm.HieroGrammar;

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunctionList;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

public class SamtFormatReader extends GrammarReader<BilingualRule> {

	static {
		fieldDelimiter = "#";
		nonTerminalRegEx = "^@[A-Z\\-+]+";
		nonTerminalCleanRegEx = "@";
		description = "Original SAMT format";
	}
	
	public SamtFormatReader(String grammarFile, SymbolTable vocabulary,
			FeatureFunctionList features) {
		super(grammarFile, vocabulary, features);
	}

	@Override
	protected BilingualRule parseLine(String line) {
		return null;
	}

	@Override
	public String toTokenIds(BilingualRule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toTokenIdsWithoutFeatureScores(BilingualRule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toWords(BilingualRule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toWordsWithoutFeatureScores(BilingualRule rule) {
		// TODO Auto-generated method stub
		return null;
	}
}
