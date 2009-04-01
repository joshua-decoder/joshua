package joshua.sarray;


public interface MatchedHierarchicalPhrases {

	public PrefixTree getPrefixTree();
	
	public int getNumberOfTerminalSequences();
	
	public HierarchicalPhrase get(int phraseIndex);
		
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getStartPosition(int phraseIndex, int positionNumber);
	
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getEndPosition(int phraseIndex, int positionNumber);

	
	/**
	 * Gets the number of locations in the corpus 
	 * that match the pattern.
	 * 
	 * @return The number of locations in the corpus 
	 * that match the pattern.
	 */
	public int size();
	
	
	public boolean isEmpty();
	
	/** 
	 * Gets the index of the sentence from which the specified phrase was extracted.
	 *  
	 * @param phraseIndex Index of a phrase
	 * @return the index of the sentence from which the specified phrase was extracted.
	 */
	public int getSentenceNumber(int phraseIndex);
	
	public Pattern getPattern();
	
	public int getTerminalSequenceLength(int i);
	
	public MatchedHierarchicalPhrases copyWith(Pattern pattern);
	
}
