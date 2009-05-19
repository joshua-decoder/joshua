package joshua.corpus.lexprob;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.util.Pair;

/**
 * Represents lexical probability distributions in both directions.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface LexicalProbabilities {

	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	float sourceGivenTarget(Integer sourceWord, Integer targetWord);
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
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

	Pair<Float,Float> calculateLexProbs(MatchedHierarchicalPhrases sourcePhrase, int sourcePhraseIndex);
	
}
