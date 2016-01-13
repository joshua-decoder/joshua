package joshua.decoder.ff.lm.berkeley_lm;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.decoder.Decoder;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.WordIndexer;
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

    if (!new File(lm_file).exists()) {
      System.err.println("Can't read lm_file '" + lm_file + "'");
      System.exit(1);
    }

    if (logRequests) {
      logger.addHandler(logHandler);
      logger.setLevel(Level.FINEST);
      logger.setUseParentHandlers(false);
    }

    try { // try binary format (even gzipped)
      lm = (ArrayEncodedNgramLanguageModel<String>) LmReaders.<String>readLmBinary(lm_file);
      Decoder.LOG(1, "Loading Berkeley LM from binary " + lm_file);
    } catch (RuntimeException e) {
      ConfigOptions opts = new ConfigOptions();
      Decoder.LOG(1, "Loading Berkeley LM from ARPA file " + lm_file);
      final StringWordIndexer wordIndexer = new StringWordIndexer();
      ArrayEncodedNgramLanguageModel<String> berkeleyLm =
          LmReaders.readArrayEncodedLmFromArpa(lm_file, false, wordIndexer, opts, order);

      lm = ArrayEncodedCachingLmWrapper.wrapWithCacheThreadSafe(berkeleyLm);
    }
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
    vocabIdToMyIdMapping[id] = myid;

    return false;
  }

  @Override
  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    if (sentence == null) return 0;
    int sentenceLength = sentence.length;
    if (sentenceLength <= 0) return 0;

    float probability = 0;
    // partial ngrams at the begining
    for (int j = startIndex; j < order && j <= sentenceLength; j++) {
      // TODO: startIndex dependens on the order, e.g., this.ngramOrder-1 (in srilm, for 3-gram lm,
      // start_index=2. othercase, need to check)
      int[] ngram = Arrays.copyOfRange(sentence, 0, j);
      double logProb = ngramLogProbability_helper(ngram, false);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    // regular-order ngrams
    for (int i = 0; i <= sentenceLength - order; i++) {
      int[] ngram = Arrays.copyOfRange(sentence, i, i + order);
      double logProb =  ngramLogProbability_helper(ngram, false);
      if (logger.isLoggable(Level.FINE)) {
        String words = Vocabulary.getWords(ngram);
        logger.fine("\tlogp ( " + words + " )  =  " + logProb);
      }
      probability += logProb;
    }

    return probability;
  }

  @Override
  public float ngramLogProbability_helper(int[] ngram, int order) {
    return ngramLogProbability_helper(ngram, false);
  }
  
  protected float ngramLogProbability_helper(int[] ngram, boolean log) {

    int[] mappedNgram = arrayScratch.get();
    if (mappedNgram.length < ngram.length) {
      arrayScratch.set(mappedNgram = new int[mappedNgram.length * 2]);
    }
    for (int i = 0; i < ngram.length; ++i) {
      mappedNgram[i] = vocabIdToMyIdMapping[ngram[i]];
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

  @Override
  public float ngramLogProbability(int[] ngram) {
    return ngramLogProbability_helper(ngram,true);
  }

  @Override
  public float ngramLogProbability(int[] ngram, int order) {
    return ngramLogProbability(ngram);
  }
}
