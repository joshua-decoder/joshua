package joshua.decoder.ff.lm;

import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.state_maintenance.KenLMState;

/**
 * JNI wrapper for KenLM. This version of KenLM supports two use cases, implemented by the separate
 * feature functions KenLMFF and LanguageModelFF. KenLMFF uses the RuleScore() interface in
 * lm/left.hh, returning a state pointer representing the KenLM state, while LangaugeModelFF handles
 * state by itself and just passes in the ngrams for scoring.
 * 
 * @author Kenneth Heafield
 * @author Matt Post <post@cs.jhu.edu>
 */

public class KenLM implements NGramLanguageModel, Comparable<KenLM> {

  static {
    try {
      System.loadLibrary("ken");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("* FATAL: Can't find libken.so (libken.dylib on OS X) in $JOSHUA/lib");
      System.err.println("*        This probably means that the KenLM library didn't compile.");
      System.err.println("*        Make sure that BOOST_ROOT is set to the root of your boost");
      System.err.println("*        installation (it's not /opt/local/, the default), change to");
      System.err.println("*        $JOSHUA, and type 'ant kenlm'. If problems persist, see the");
      System.err.println("*        website (joshua-decoder.org).");
      System.exit(1);
    }
  }

  private final long pointer;

  // this is read from the config file, used to set maximum order
  private final int ngramOrder;
  // inferred from model file (may be larger than ngramOrder)
  private final int N;
  // whether left-state minimization was requested
  private boolean minimizing;

  private final static native long construct(String file_name);

  private final static native void destroy(long ptr);

  private final static native int order(long ptr);

  private final static native boolean registerWord(long ptr, String word, int id);

  private final static native float prob(long ptr, int words[]);

  private final static native StateProbPair probRule(long ptr, long pool, long words[]);
  
  private final static native float estimateRule(long ptr, long words[]);

  private final static native float probString(long ptr, int words[], int start);

  public final static native long createPool();
  public final static native void destroyPool(long pointer);

  public KenLM(int order, String file_name) {
    ngramOrder = order;

    pointer = construct(file_name);
    N = order(pointer);
  }

  public void destroy() {
    destroy(pointer);
  }

  public int getOrder() {
    return ngramOrder;
  }

  public boolean registerWord(String word, int id) {
    return registerWord(pointer, word, id);
  }

  public float prob(int words[]) {
    return prob(pointer, words);
  }

  // Apparently Zhifei starts some array indices at 1. Change to 0-indexing.
  public float probString(int words[], int start) {
    return probString(pointer, words, start - 1);
  }

  /**
   * This function is the bridge to the interface in kenlm/lm/left.hh, which has KenLM score the
   * whole rule. It takes a list of words and states retrieved from tail nodes (nonterminals in the
   * rule). Nonterminals have a negative value so KenLM can distinguish them. The sentence number is
   * needed so KenLM knows which memory pool to use. When finished, it returns the updated KenLM
   * state and the LM probability incurred along this rule.
   * 
   * @param words
   * @param sentId
   * @return
   */
  public StateProbPair probRule(long[] words, long poolPointer) {

    StateProbPair pair = null;
    try {
      pair = probRule(pointer, poolPointer, words);
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
      System.exit(1);
    }

    return pair;
  }

  /**
   * Public facing function that estimates the cost of a rule, which value is used for sorting
   * rules during cube pruning.
   * 
   * @param words
   * @return the estimated cost of the rule (the (partial) n-gram probabilities of all words in the rule)
   */
  public float estimateRule(long[] words) {
    float estimate = 0.0f;
    try {
      estimate = estimateRule(pointer, words);
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    return estimate;
  }


  /**
   * Inner class used to hold the results returned from KenLM with left-state minimization. Note
   * that inner classes have to be static to be accessible from the JNI!
   */
  public static class StateProbPair {
    public KenLMState state = null;
    public float prob = 0.0f;

    public StateProbPair(long state, float prob) {
      this.state = new KenLMState(state);
      this.prob = prob;
    }
  }

  @Override
  public int compareTo(KenLM other) {
    if (this == other)
      return 0;
    else
      return -1;
  }

  /**
   * These functions are used if KenLM is invoked under LanguageModelFF instead of KenLMFF.
   */
  @Override
  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    return probString(sentence, startIndex);
  }

  @Override
  public float ngramLogProbability(int[] ngram, int order) {
    if (order != N && order != ngram.length)
      throw new RuntimeException("Lower order not supported.");
    return prob(ngram);
  }

  @Override
  public float ngramLogProbability(int[] ngram) {
    return prob(ngram);
  }
}
