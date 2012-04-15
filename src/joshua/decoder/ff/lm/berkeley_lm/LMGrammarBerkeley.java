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
import java.util.logging.Logger;

import joshua.decoder.ff.lm.AbstractLM;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.io.LmReaders;

/**
 * This class wraps Berkeley LM.
 * 
 * @author adpauls@gmail.com
 */
public class LMGrammarBerkeley extends AbstractLM {

  private ArrayEncodedNgramLanguageModel<String> lm;

  private static final Logger logger = Logger.getLogger(LMGrammarBerkeley.class.getName());

  private int[] vocabIdToMyIdMapping;

  private int mappingLength = 0;

  public LMGrammarBerkeley(int order, String lm_file) {
    super(order);
    vocabIdToMyIdMapping = new int[10];

    ConfigOptions opts = new ConfigOptions();

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

    if (fileIsBinary) {
      logger.info("Loading Berkeley LM from binary " + lm_file);
      lm = (ArrayEncodedNgramLanguageModel<String>) LmReaders.<String>readLmBinary(lm_file);
    } else {

      logger.info("Loading Berkeley LM from ARPA file " + lm_file);
      final StringWordIndexer wordIndexer = new StringWordIndexer();
      ArrayEncodedNgramLanguageModel<String> berkeleyLm =
          LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

      // this is how you would wrap with a cache
      // ArrayEncodedNgramLanguageModel<String> berkeleyLm = new
      // ArrayEncodedCachingLmWrapper<String>(LmReaders.readArrayEncodedLmFromArpa(lm_file, false,
      // wordIndexer, opts, order));
      lm = berkeleyLm;
    }
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
    vocabIdToMyIdMapping[id] = myid;

    return false;
  }

  @Override
  protected double ngramLogProbability_helper(int[] ngram, int order) {
    // Adam Pauls: having to make a copy might be very inefficient
    // if this shows up in the profiles, I might have to expose more
    // of the lm interface to avoid the copy.
    final int[] copyOf = Arrays.copyOf(ngram, ngram.length);
    for (int i = 0; i < ngram.length; ++i) {
      copyOf[i] = ngram[i] >= mappingLength ? -1 : vocabIdToMyIdMapping[ngram[i]];
    }
    final float res = lm.getLogProb(copyOf, 0, copyOf.length);

    return res;
  }

  @Override
  protected double logProbabilityOfBackoffState_helper(int[] ngram, int order,
      int qtyAdditionalBackoffWeight) {
    throw new UnsupportedOperationException(
        "probabilityOfBackoffState_helper undefined for Berkeley lm");
  }

}
