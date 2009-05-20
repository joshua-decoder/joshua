package joshua.corpus.lexprob;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.util.Pair;

/**
 * Represents lexical probability distributions in both directions.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface LexicalProbabilities {

	
	/**
	 * Gets the lexical translation probability 
	 * of the source word given the target word.
	 * 
	 * @param sourceWord Source language word symbol
	 * @param targetWord Target language word symbol
	 * @return the lexical translation probability 
	 *         of the source word given the target word
	 */
	float sourceGivenTarget(Integer sourceWord, Integer targetWord);
	
	/**
	 * Gets the lexical translation probability 
	 * of the target word given the source word.
	 * 
	 * @param targetWord Target language word symbol
	 * @param sourceWord Source language word symbol
	 * @return the lexical translation probability 
	 *         of the target word given the source word
	 */
	float targetGivenSource(Integer targetWord, Integer sourceWord);
	
	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	float sourceGivenTarget(String sourceWord, String targetWord);
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	float targetGivenSource(String targetWord, String sourceWord);

	Pair<Float,Float> calculateLexProbs(MatchedHierarchicalPhrases sourcePhrase, int sourcePhraseIndex, HierarchicalPhrase targetPhrase);
	
}
