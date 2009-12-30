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

import joshua.decoder.Support;
import joshua.corpus.vocab.SymbolTable;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a default implementation for the Equivalent
 * LM State optimization (namely, don't back off anywhere). It also
 * provides some default implementations for more general functions
 * on the interface to fall back to more specific ones (e.g. from
 * ArrayList<Integer> to int[]) and a default implementation for
 * sentenceLogProbability which enumerates the n-grams and calls
 * calls ngramLogProbability for each of them.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public abstract class DefaultNGramLanguageModel implements NGramLanguageModel {
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(DefaultNGramLanguageModel.class.getName());
	
	protected final SymbolTable symbolTable;
	protected final int         ngramOrder;
	
//===============================================================
// Constructors
//===============================================================
	public DefaultNGramLanguageModel(SymbolTable symbolTable, int order) {
		this.symbolTable = symbolTable;
		this.ngramOrder  = order;
	}
	
	
//===============================================================
// Attributes
//===============================================================
	public final int getOrder() {
		return this.ngramOrder;
	}
	
	
//===============================================================
// NGramLanguageModel Methods
//===============================================================

	public double sentenceLogProbability(
		List<Integer> sentence, int order, int startIndex
	) {
		if (sentence==null) return 0.0;
		int sentenceLength = sentence.size();
		if (sentenceLength <= 0) return 0.0;
		
		double probability = 0.0;
		// partial ngrams at the begining
		for (int j = startIndex; j < order && j <= sentenceLength; j++) {
			//TODO: startIndex dependents on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm, start_index=2. othercase, need to check)
			int[] ngram = Support.subIntArray(sentence, 0, j);
			double logProb = ngramLogProbability(ngram, order);
			if (logger.isLoggable(Level.FINE)) {
				String words = symbolTable.getWords(ngram);
				logger.fine("\tlogp ( " + words + " )  =  " + logProb);
			}
			probability += logProb;
		}
		
		// regular-order ngrams
		for (int i = 0; i <= sentenceLength - order; i++) {
			int[] ngram = Support.subIntArray(sentence, i, i + order);
			double logProb = ngramLogProbability(ngram, order);
			if (logger.isLoggable(Level.FINE)) {
				String words = symbolTable.getWords(ngram);
				logger.fine("\tlogp ( " + words + " )  =  " + logProb);
			}
			probability += logProb;
		}
		
		return probability;
	}
	
	
	/** @deprecated this function is much slower than the int[] version */
	@Deprecated
	public double ngramLogProbability(List<Integer> ngram, int order) {
		return ngramLogProbability(
			Support.subIntArray(ngram, 0, ngram.size()), order);
	}
	
	
	public double ngramLogProbability(int[] ngram) {
		return this.ngramLogProbability(ngram, this.ngramOrder);
	}
	
	public abstract double ngramLogProbability(int[] ngram, int order);
	
	
	/**
	 * Will never be called, because BACKOFF_LEFT_LM_STATE_SYM_ID
	 * token will never exist. However, were it to be called,
	 * it should return a probability of 1 (logprob of 0).
	 */
	public double logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
		return 0; // log(1) == 0;
	}
	
	/**
	 * Will never be called, because BACKOFF_LEFT_LM_STATE_SYM_ID
	 * token will never exist. However, were it to be called,
	 * it should return a probability of 1 (logprob of 0).
	 */
	public double logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
		return 0; // log(1) == 0;
	}
	
	
	public int[] leftEquivalentState(int[] originalState, int order, double[] cost) {
		return originalState;
	}
	
	
	public int[] rightEquivalentState(int[] originalState, int order) {
		return originalState;
	}
}
