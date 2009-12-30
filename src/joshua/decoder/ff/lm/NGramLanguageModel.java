/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package joshua.decoder.ff.lm;

// BUG: At best we should use List, but we use int[] everywhere to
// represent phrases therefore these additional methods are excessive.
import java.util.List;

/**
 * An interface for new language models to implement. An object of
 * this type is passed to LanguageModelFF, which will handle all
 * the dynamic programming and state maintinence.
 *
 * All the function here should return LogP, not the cost.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface NGramLanguageModel {
	
//===============================================================
// Attributes
//===============================================================
	int getOrder();
	
//===============================================================
// Methods
//===============================================================

	// BUG: why do we pass the order? Does this method reduce the order as well?
	/**
	 * @param sentence   the sentence to be scored
	 * @param order      the order of N-grams for the LM
	 * @param startIndex the index of first event-word we want
	 *                   to get its probability; if we want to
	 *                   get the prob for the whole sentence,
	 *                   then startIndex should be 1
	 * @return the LogP of the whole sentence
	 */
	double sentenceLogProbability(List<Integer> sentence, int order, int startIndex);
	
	
	/**
	 * @param order used to temporarily reduce the order used
	 *              by the model.
	 */
	double ngramLogProbability(List<Integer> ngram, int order);
	double ngramLogProbability(int[] ngram, int order);
	double ngramLogProbability(int[] ngram);
	
	
//===============================================================
// Equivalent LM State (use DefaultNGramLanguageModel if you don't care)
//===============================================================
	
	/**
	 * This returns the log probability of the special backoff
	 * symbol used to fill out contexts which have been backed-off.
	 * The LanguageModelFF implementation is to call this unigram
	 * probability for each such token, and then call
	 * ngramLogProbability for the remaining actual N-gram.
	 */
	//TODO Is this really the best interface?
	double logProbOfBackoffState(
		List<Integer> ngram, int order, int qtyAdditionalBackoffWeight);
	
	double logProbabilityOfBackoffState(
		int[] ngram, int order, int qtyAdditionalBackoffWeight);
	
	int[] leftEquivalentState(int[] originalState, int order, double[] cost);
	int[] rightEquivalentState(int[] originalState, int order);
	
}
