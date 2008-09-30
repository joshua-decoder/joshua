package joshua.suffix_array;

import java.util.Arrays;

import edu.jhu.sa.util.sentence.Phrase;
import edu.jhu.sa.util.sentence.Vocabulary;


/**
 * Represents a pattern of terminals and nonterminals.
 * <p>
 * The integer representation of each terminal must be positive.
 * The integer representation of each nonterminal must be negative.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class Pattern extends BasicPhrase {

//===============================================================
// Member variables
//===============================================================


	final int arity;
	final SuffixCase suffixCase;
	final PrefixCase prefixCase;


//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructs a pattern of terminals and nonterminals.
	 * <p>
	 * The integer representation of each terminal must be positive.
	 * The integer representation of each nonterminal must be negative.
	 * 
	 * @param vocab Vocabulary capable of mapping between symbols and integers.
	 */
	public Pattern(Vocabulary vocab, int... words) {
		super(words, vocab);
		
		this.arity = calculateArity(this.words);
		
		this.suffixCase = suffixCase(words);
		this.prefixCase = prefixCase(words);
	}
	
	/**
	 * Constructs a pattern by copying an existing phrase.
	 * 
	 * @param phrase an existing phrase
	 */
	public Pattern(Phrase phrase) {
		this.words = new int[phrase.size()];
		this.vocab = phrase.getVocab();
		
		for(int i = 0 ; i < phrase.size(); i++) {
			words[i] = phrase.getWordID(i);
		}
		this.arity = calculateArity(this.words);
		
		this.suffixCase = suffixCase(words);
		this.prefixCase = prefixCase(words);
	}
	
	//TODO What does this constructor do?
	public Pattern(Pattern pattern, int... word) {
		super(PrefixTree.pattern(pattern.words, word),pattern.vocab);
		this.arity = calculateArity(this.words);
		
		this.suffixCase = suffixCase(words);
		this.prefixCase = prefixCase(words);
	}
	

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public boolean startsWithNonTerminal() {
		// we assume that the nonterminal symbols will be denoted with negative numbers
		return words[0] < 0;
	}
	
	
	public boolean endsWithNonTerminal() {
		// we assume that the nonterminal symbols will be denoted with negative numbers
		return words[words.length-1] < 0;
	}
	
	//===========================================================
	// Methods
	//===========================================================

	public int arity() {
		return arity;
	}
	
	public String toString() {
		return Arrays.toString(words);
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================


	int[] getWords() {
		return words;
	}
	
	enum PrefixCase {
		EMPTY_PREFIX,
		ENDS_WITH_NONTERMINAL,
		ENDS_WITH_TWO_TERMINALS,
		ENDS_WITH_NONTERMINAL_TERMINAL
	}
	
	enum SuffixCase {
		EMPTY_SUFFIX,
		STARTS_WITH_NONTERMINAL,
		STARTS_WITH_TWO_TERMINALS,
		STARTS_WITH_TERMINAL_NONTERMINAL
	}
	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
	private int calculateArity(int[] words) {
		
		int arity = 0;
		
		for (int element : words) {
			if (element==PrefixTree.X) arity++;
		}
		
		return arity;
	}
	
	
	private PrefixCase prefixCase(int[] words) {
		
		if (words==null || words.length==0)
			return PrefixCase.EMPTY_PREFIX;
		
		if (words[words.length-1] == PrefixTree.X) {
			
			return PrefixCase.ENDS_WITH_NONTERMINAL;
			
		} else {
			
			if (words.length > 1) {
				
				if (words[words.length-2] == PrefixTree.X) {
					
					return PrefixCase.ENDS_WITH_NONTERMINAL_TERMINAL;
					
				} else {
					
					return PrefixCase.ENDS_WITH_TWO_TERMINALS;
					
				}
				
			} else {
				
				return PrefixCase.EMPTY_PREFIX;
				
			}
			
		}
		
	}
	
	
	private SuffixCase suffixCase(int[] words) {
		
		if (words==null || words.length==0)
			return SuffixCase.EMPTY_SUFFIX;;
		
		if (words[0] == PrefixTree.X) {
		
			return SuffixCase.STARTS_WITH_NONTERMINAL;
		
		} else {
			
			if (words.length > 1) {
				
				if (words[1] == PrefixTree.X) {
					
					return SuffixCase.STARTS_WITH_TERMINAL_NONTERMINAL;
					
				} else {
					
					return SuffixCase.STARTS_WITH_TWO_TERMINALS;
					
				}
				
				
			} else {
				
				return SuffixCase.EMPTY_SUFFIX;
				
			}
			
		}
		
	}
	
}
