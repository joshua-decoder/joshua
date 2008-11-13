package joshua.util.lexprob;

import joshua.sarray.HierarchicalPhrase;
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
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord);
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	public float targetGivenSource(Integer targetWord, Integer sourceWord);
	
	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	public float sourceGivenTarget(String sourceWord, String targetWord);
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	public float targetGivenSource(String targetWord, String sourceWord);

	public Pair<Float, Float> calculateLexProbs(HierarchicalPhrase sourcePhrase);
	
}
