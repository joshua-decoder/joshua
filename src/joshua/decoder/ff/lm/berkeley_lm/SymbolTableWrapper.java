package joshua.decoder.ff.lm.berkeley_lm;

import joshua.corpus.Vocabulary;
import edu.berkeley.nlp.lm.WordIndexer;

class SymbolTableWrapper implements WordIndexer<String>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String startSymbol;

	private String endSymbol;

	private String unkSymbol;

	int size = -1;

	public SymbolTableWrapper() {

	}

	@Override
	public int getOrAddIndex(String word) {
		return Vocabulary.id(word);
	}

	@Override
	public int getOrAddIndexFromString(String word) {
		return Vocabulary.id(word);
	}

	@Override
	public String getWord(int index) {
		return Vocabulary.word(index);
	}

	@Override
	public int numWords() {
		return Vocabulary.size();
	}

	@Override
	public String getStartSymbol() {
		return startSymbol;
	}

	@Override
	public String getEndSymbol() {
		return endSymbol;
	}

	@Override
	public String getUnkSymbol() {
		return unkSymbol;
	}

	@Override
	public void setStartSymbol(String sym) {
		startSymbol = sym;
	}

	@Override
	public void setEndSymbol(String sym) {
		endSymbol = sym;
	}

	@Override
	public void setUnkSymbol(String sym) {
		unkSymbol = sym;
	}

	@Override
	public void trimAndLock() {

	}

	@Override
	public int getIndexPossiblyUnk(String word) {
		return Vocabulary.id(word);
	}

}