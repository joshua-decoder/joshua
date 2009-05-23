package joshua.corpus.suffix_array;

/**
 * Represents a data type that knows about the format of a pattern.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface PatternFormat {

	/** 
	 * Returns <code>true</code> if the last element in the
	 * pattern is a nonterminal.
	 *
	 * @return <code>true</code> if the last element in the
	 *         pattern is a nonterminal, <code>false</code>
	 *         otherwise
	 */
	boolean endsWithNonterminal();
	
	/** 
	 * Returns <code>true</code> if the first element in the
	 * pattern is a nonterminal.
	 *
	 * @return <code>true</code> if the first element in the
	 *         pattern is a nonterminal, <code>false</code>
	 *         otherwise
	 */
	boolean startsWithNonterminal();
	
	/** 
	 * Returns <code>true</code> if the last two elements in
	 * the pattern are terminals.
	 *
	 * @return <code>true</code> if the last two elements in
	 *         the pattern are terminals, <code>false</code>
	 *         otherwise
	 */
	boolean endsWithTwoTerminals();
	
	/** 
	 * Returns <code>true</code> if the second element in the
	 * pattern is a terminal.
	 *
	 * @return <code>true</code> if the second element in the
	 *         pattern is a terminal, <code>false</code> otherwise
	 */
	boolean secondTokenIsTerminal();
	
	
	/**
	 * Returns the number of nonterminals in the pattern.
	 *
	 * @return the number of nonterminals in the pattern
	 */
	int arity();
}
