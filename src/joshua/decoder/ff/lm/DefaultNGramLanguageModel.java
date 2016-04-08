/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder.ff.lm;

import java.util.Arrays;
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
 */
public abstract class DefaultNGramLanguageModel implements NGramLanguageModel {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(DefaultNGramLanguageModel.class.getName());

  protected final int ngramOrder;
  
  protected float ceiling_cost = -100;

  // ===============================================================
  // Constructors
  // ===============================================================
  public DefaultNGramLanguageModel(int order, float ceiling_cost) {
    this.ngramOrder = order;
    this.ceiling_cost = ceiling_cost;
  }

  public DefaultNGramLanguageModel(int order) {
    this.ngramOrder = order;
  }


  // ===============================================================
  // Attributes
  // ===============================================================
  @Override
  public final int getOrder() {
    return this.ngramOrder;
  }


  // ===============================================================
  // NGramLanguageModel Methods
  // ===============================================================

  @Override
  public boolean registerWord(String token, int id) {
    // No private LM ID mapping, do nothing
    return false;
  }

  @Override
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

  @Override
  public float ngramLogProbability(int[] ngram) {
    return this.ngramLogProbability(ngram, this.ngramOrder);
  }

  protected abstract float ngramLogProbability_helper(int[] ngram, int order);
  
  @Override
  public float ngramLogProbability(int[] ngram, int order) {
    if (ngram.length > order) {
      throw new RuntimeException("ngram length is greather than the max order");
    }
    // if (ngram.length==1 && "we".equals(Vocabulary.getWord(ngram[0]))) {
    // System.err.println("Something weird is about to happen");
    // }

    int historySize = ngram.length - 1;
    if (historySize >= order || historySize < 0) {
      // BUG: use logger or exception. Don't zero default
      throw new RuntimeException("Error: history size is " + historySize);
      // return 0;
    }
    float probability = ngramLogProbability_helper(ngram, order);
    if (probability < ceiling_cost) {
      probability = ceiling_cost;
    }
    return probability; 
  }
}
