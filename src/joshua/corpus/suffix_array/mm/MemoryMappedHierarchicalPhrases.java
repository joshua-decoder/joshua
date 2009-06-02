package joshua.corpus.suffix_array.mm;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Span;
import joshua.corpus.suffix_array.Pattern;

public class MemoryMappedHierarchicalPhrases implements
		MatchedHierarchicalPhrases {

	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public Pattern getPattern() {
		return null;
	}
	
	public boolean containsTerminalAt(int phraseIndex, int alignmentPointIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	public int arity() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getEndPosition(int phraseIndex, int positionNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getFirstTerminalIndex(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getLastTerminalIndex(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getNumberOfTerminalSequences() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getSentenceNumber(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Span getSpan(int phraseIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getStartPosition(int phraseIndex, int positionNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getTerminalSequenceEndIndex(int phraseIndex, int sequenceIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getTerminalSequenceLength(int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getTerminalSequenceStartIndex(int phraseIndex, int sequenceIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean endsWithNonterminal() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean endsWithTwoTerminals() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean secondTokenIsTerminal() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean startsWithNonterminal() {
		// TODO Auto-generated method stub
		return false;
	}

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public MatchedHierarchicalPhrases copyWithFinalX() {
		// TODO Auto-generated method stub
		return null;
	}

	public MatchedHierarchicalPhrases copyWithInitialX() {
		// TODO Auto-generated method stub
		return null;
	}


}
