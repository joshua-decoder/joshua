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

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.corpus.SymbolTable;

import java.util.ArrayList;

/**
 * this class implements 
 * (1) LMGrammar interface 
 *
 * All the functions here returns LogP, not the cost.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-17 01:41:03 -0400 (星期五, 17 十月 2008) $
 */
public abstract class DefaultNGramLanguageModel implements NGramLanguageModel {
	
	protected final SymbolTable symbolTable;
	protected final int         ngramOrder;
	
	
	public DefaultNGramLanguageModel(SymbolTable symbolTable, int order) {
		this.symbolTable = symbolTable;
		this.ngramOrder  = order;
	}
	
	
	public final int getOrder() {
		return this.ngramOrder;
	}
	
	
	/**
	 * @param sentence   the sentence to be scored
	 * @param order      the order of N-grams for the LM
	 * @param startIndex the index of first event-word we want
	 *                   to get its probability; if we want to
	 *                   get the prob for the whole sentence,
	 *                   then startIndex should be 1
	 * @return the LogP of the whole sentence
	 */
	public final double sentenceLogProbability(ArrayList<Integer> sentence,
		int order, int startIndex
	) {
		double probability = 0.0;
		int sentenceLength = sentence.size();
		if (null == sentence || sentenceLength <= 0) {
			return probability;
		}
		
		// partial ngrams at the begining
		for (int j = startIndex; j < order && j <= sentenceLength; j++) {
			//TODO: startIndex dependents on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm, start_index=2. othercase, need to check)
			probability += ngramLogProbability(
				Support.sub_int_array(sentence, 0, j), order);
		}
		
		// regular-order ngrams
		for (int i = 0; i <= sentenceLength - order; i++) {
			probability += ngramLogProbability(
				Support.sub_int_array(sentence, i, i + order), order);
		}
		
		return probability;
	}
	
	
	/** @deprecated this function is much slower than the int[] version */
	public final double ngramLogProbability(ArrayList<Integer> ngram, int order) {
		return ngramLogProbability(
			Support.sub_int_array(ngram, 0, ngram.size()), order);
	}
	
	
	public final double ngramLogProbability(int[] ngram) {
		return this.ngramLogProbability(ngram, this.ngramOrder);
	}
	
	public final double ngramLogProbability(int[] ngram, int order) {
		if (ngram.length > order) {
			throw new RuntimeException("ngram length is greather than the max order");
		}
		int historySize = ngram.length - 1;
		if (historySize >= order || historySize < 0) {
			// BUG: use logger or exception. Don't zero default
			System.out.println("Error: history size is " + historySize);
			return 0;
		}
		double probability = ngramLogProbability_helper(ngram, order);
		if (probability < -JoshuaConfiguration.lm_ceiling_cost) {
			probability = -JoshuaConfiguration.lm_ceiling_cost;
		}
		return probability;
	}
	
	protected abstract double ngramLogProbability_helper(int[] ngram, int order);
	
	
	/**
	 * called by LMModel to calculate additional bow for  BACKOFF_LEFT_LM_STATE_SYM_ID.
	 * @deprecated this function is much slower than the int[] version
	 */
	public final double probabilityOfBackoffState(ArrayList<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
		return probabilityOfBackoffState(
			Support.sub_int_array(ngram, 0, ngram.size()),
			order, qtyAdditionalBackoffWeight);
	}
	
	
	public final double probabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
		if (ngram.length > order) {
			throw new RuntimeException("ngram length is greather than the max order");
		}
		if (ngram[ngram.length-1] != LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID) {
			throw new RuntimeException("last wrd is not <bow>");
		}
		if (qtyAdditionalBackoffWeight > 0) {
			return probabilityOfBackoffState_helper(
				ngram, order, qtyAdditionalBackoffWeight);
		} else {
			return 0.0;
		}
	}
	
	
	protected abstract double probabilityOfBackoffState_helper(
		int[] ngram, int order, int qtyAdditionalBackoffWeight);
	
	
	// BUG: We should have different classes based on the configuration in use
	public int[] leftEquivalentState(int[] originalState, int order,
		double[] cost
	) {
		if (JoshuaConfiguration.use_left_equivalent_state)
			throw new UnsupportedOperationException("getLeftEquivalentState is not overwritten by a concrete class");
		
		return originalState;
	}
	
	// BUG: We should have different classes based on the configuration in use
	public int[] rightEquivalentState(int[] originalState, int order) {
		if ( !JoshuaConfiguration.use_right_equivalent_state
		|| originalState.length != this.ngramOrder-1) {
			return originalState;
		} else {
			throw new UnsupportedOperationException("getRightEquivalentState is not overwritten by a concrete class");
		}
	}
}
