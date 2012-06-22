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
package joshua.decoder.ff.lm.berkeley_lm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.ff.lm.AbstractLM;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.decoder.ff.lm.NGramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.ContextEncodedNgramLanguageModel.LmContextInfo;
import edu.berkeley.nlp.lm.cache.ArrayEncodedCachingLmWrapper;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.util.StrUtils;

/**
 * This class wraps Berkeley LM.
 * 
 * @author adpauls@gmail.com
 */
public class LMGrammarBerkeley extends DefaultNGramLanguageModel {



  private ArrayEncodedNgramLanguageModel<String> lm;

  private static final Logger logger = Logger.getLogger(LMGrammarBerkeley.class.getName());

  private int[] vocabIdToMyIdMapping;

  private ThreadLocal<int[]> arrayScratch = new ThreadLocal<int[]>() {

    @Override
    protected int[] initialValue() {
      return new int[5];
    }
  };

  private int mappingLength = 0;

  private final int unkIndex;

  private static boolean logRequests = false;

  private static Handler logHandler = null;

  public LMGrammarBerkeley(int order, String lm_file) {
    super(order);
    vocabIdToMyIdMapping = new int[10];


    // determine whether the file is in its binary format
    boolean fileIsBinary = true;
    try {
      ObjectInputStream in =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(lm_file))));
    } catch (StreamCorruptedException e) {
      fileIsBinary = false;
    } catch (IOException e) {
      System.err.println("Can't read file '" + lm_file + "'");
      System.exit(1);
    }

    if (logRequests) {
      logger.addHandler(logHandler);
      logger.setLevel(Level.FINEST);
      logger.setUseParentHandlers(false);
    }
    if (fileIsBinary) {
      logger.info("Loading Berkeley LM from binary " + lm_file);
      lm = (ArrayEncodedNgramLanguageModel<String>) LmReaders.<String>readLmBinary(lm_file);
    } else {
      ConfigOptions opts = new ConfigOptions();
      opts.unknownWordLogProb = -1.0f * JoshuaConfiguration.lm_ceiling_cost;
      logger.info("Loading Berkeley LM from ARPA file " + lm_file);
      final StringWordIndexer wordIndexer = new StringWordIndexer();
      ArrayEncodedNgramLanguageModel<String> berkeleyLm =
          LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

      lm = ArrayEncodedCachingLmWrapper.wrapWithCacheThreadSafe(berkeleyLm);
    }
    lm.setOovWordLogProb((float) (-1.0f * JoshuaConfiguration.lm_ceiling_cost));
    this.unkIndex = lm.getWordIndexer().getOrAddIndex(lm.getWordIndexer().getUnkSymbol());
  }

  @Override
  public boolean registerWord(String token, int id) {
    int myid = lm.getWordIndexer().getIndexPossiblyUnk(token);
    if (myid < 0) return false;
    if (id >= vocabIdToMyIdMapping.length) {
      vocabIdToMyIdMapping =
          Arrays.copyOf(vocabIdToMyIdMapping, Math.max(id + 1, vocabIdToMyIdMapping.length * 2));

    }
    mappingLength = Math.max(mappingLength, id + 1);
    vocabIdToMyIdMapping[id] = myid == unkIndex ? -1 : myid;

    return false;
  }

  @Override
  public double sentenceLogProbability(List<Integer> sentence, int order, int startIndex) {
    if (sentence == null) return 0.0;
    int sentenceLength = sentence.size();
    if (sentenceLength <= 0) return 0.0;

    double probability = 0.0;
    // partial ngrams at the begining
    for (int j = startIndex; j < order && j <= sentenceLength; j++) {
      // TODO: startIndex dependents on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm,
      // start_index=2. othercase, need to check)
      int[] ngram = Support.subIntArray(sentence, 0, j);
      double logProb = ngramLogProbability_helper(ngram, false);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    // regular-order ngrams
    for (int i = 0; i <= sentenceLength - order; i++) {
      int[] ngram = Support.subIntArray(sentence, i, i + order);
      double logProb =  ngramLogProbability_helper(ngram, false);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    return probability;
  }

  protected double ngramLogProbability_helper(int[] ngram, boolean log) {

    int[] mappedNgram = arrayScratch.get();
    if (mappedNgram.length < ngram.length) {
      arrayScratch.set(mappedNgram = new int[mappedNgram.length * 2]);
    }
    for (int i = 0; i < ngram.length; ++i) {
      mappedNgram[i] =
          (ngram[i] == unkIndex || ngram[i] >= mappingLength) ? -1 : vocabIdToMyIdMapping[ngram[i]];
    }

    if (log && logRequests) {
      final int[] copyOf = Arrays.copyOf(mappedNgram, ngram.length);
      for (int i = 0; i < copyOf.length; ++i)
        if (copyOf[i] < 0) copyOf[i] = unkIndex;
      logger.finest(StrUtils.join(WordIndexer.StaticMethods.toList(lm.getWordIndexer(), copyOf)));
    }
    final float res = lm.getLogProb(mappedNgram, 0, ngram.length);

    return res;
  }



  public static void setLogRequests(Handler handler) {
    logRequests = true;
    logHandler = handler;

  }

  public double ngramLogProbability(int[] ngram) {
    return ngramLogProbability_helper(ngram,true);
  }

  public double logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0;
  }

  public double logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0;
  }

  public int[] leftEquivalentState(int[] originalState, int order, double[] cost) {
    return originalState;
  }

  public int[] rightEquivalentState(int[] originalState, int order) {
    return originalState;
  }

  @Override
  public double ngramLogProbability(int[] ngram, int order) {
    return ngramLogProbability(ngram);
  }



}
