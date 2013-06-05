/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.decoder.ff.lm;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;

/**
 * This class provides a default implementation for the Equivalent LM State optimization (namely,
 * don't back off anywhere). It also provides some default implementations for more general
 * functions on the interface to fall back to more specific ones (e.g. from ArrayList<Integer> to
 * int[]) and a default implementation for sentenceLogProbability which enumerates the n-grams and
 * calls calls ngramLogProbability for each of them.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public abstract class DefaultNGramLanguageModel implements NGramLanguageModel {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(DefaultNGramLanguageModel.class.getName());

  protected final int ngramOrder;

  // ===============================================================
  // Constructors
  // ===============================================================
  public DefaultNGramLanguageModel(int order) {
    this.ngramOrder = order;
  }


  // ===============================================================
  // Attributes
  // ===============================================================
  public final int getOrder() {
    return this.ngramOrder;
  }


  // ===============================================================
  // NGramLanguageModel Methods
  // ===============================================================

  public boolean registerWord(String token, int id) {
    // No private LM ID mapping, do nothing
    return false;
  }

  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    if (sentence == null) return 0.0f;
    int sentenceLength = sentence.length;
    if (sentenceLength <= 0) return 0.0f;

    float probability = 0.0f;
    // partial ngrams at the beginning
    for (int j = startIndex; j < order && j <= sentenceLength; j++) {
      // TODO: startIndex dependents on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm,
      // start_index=2. othercase, need to check)
      int[] ngram = Arrays.copyOfRange(sentence, 0, j);
      double logProb = ngramLogProbability(ngram, order);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    // regular-order ngrams
    for (int i = 0; i <= sentenceLength - order; i++) {
      int[] ngram = Arrays.copyOfRange(sentence, i, i + order);
      double logProb = ngramLogProbability(ngram, order);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    return probability;
  }

  public float ngramLogProbability(int[] ngram) {
    return this.ngramLogProbability(ngram, this.ngramOrder);
  }

  public abstract float ngramLogProbability(int[] ngram, int order);


  /**
   * Will never be called, because BACKOFF_LEFT_LM_STATE_SYM_ID token will never exist. However,
   * were it to be called, it should return a probability of 1 (logprob of 0).
   */
  public float logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0; // log(1) == 0;
  }

  /**
   * Will never be called, because BACKOFF_LEFT_LM_STATE_SYM_ID token will never exist. However,
   * were it to be called, it should return a probability of 1 (logprob of 0).
   */
  public float logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0; // log(1) == 0;
  }


  public int[] leftEquivalentState(int[] originalState, int order, double[] cost) {
    return originalState;
  }


  public int[] rightEquivalentState(int[] originalState, int order) {
    return originalState;
  }
}
