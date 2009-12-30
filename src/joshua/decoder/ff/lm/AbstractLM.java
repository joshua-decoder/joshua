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
import joshua.corpus.vocab.SymbolTable;


import java.util.List;

/**
 * This class implements NGramLanguageModel by creating wrappers
 * around the necessary functions to capture common errors. Most
 * methods are declared final, in an attempt to limit what subclasses
 * may be defined.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class AbstractLM extends DefaultNGramLanguageModel {
	
	public AbstractLM(SymbolTable symbolTable, int order) {
		super(symbolTable, order);
	}
	
	
	public final double sentenceLogProbability(
		List<Integer> sentence, int order, int startIndex
	) {
		return super.sentenceLogProbability(sentence, order, startIndex);
	}
	
	
	public final double ngramLogProbability(int[] ngram) {
		return super.ngramLogProbability(ngram);
	}
	
	
	public final double ngramLogProbability(int[] ngram, int order) {
		if (ngram.length > order) {
			throw new RuntimeException("ngram length is greather than the max order");
		}
//		if (ngram.length==1 && "we".equals(symbolTable.getWord(ngram[0]))) {
//			System.err.println("Something weird is about to happen");
//		}
		
		int historySize = ngram.length - 1;
		if (historySize >= order || historySize < 0) {
			// BUG: use logger or exception. Don't zero default
			throw new RuntimeException("Error: history size is " + historySize);
//			return 0;
		}
		double probability = ngramLogProbability_helper(ngram, order);
		if (probability < -JoshuaConfiguration.lm_ceiling_cost) {
			probability = -JoshuaConfiguration.lm_ceiling_cost;
		}
		return probability;
	}
	
	protected abstract double ngramLogProbability_helper(int[] ngram, int order);
	
	
	/**
	 * @deprecated this function is much slower than the int[]
	 *             version
	 */
	@Deprecated
	public final double logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
		return logProbabilityOfBackoffState(
			Support.subIntArray(ngram, 0, ngram.size()),
			order, qtyAdditionalBackoffWeight);
	}
	
	
	public final double logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
		if (ngram.length > order) {
			throw new RuntimeException("ngram length is greather than the max order");
		}
		if (ngram[ngram.length-1] != LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID) {
			throw new RuntimeException("last wrd is not <bow>");
		}
		if (qtyAdditionalBackoffWeight > 0) {
			return logProbabilityOfBackoffState_helper(
				ngram, order, qtyAdditionalBackoffWeight);
		} else {
			return 0.0;
		}
	}
	
	
	protected abstract double logProbabilityOfBackoffState_helper(
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
