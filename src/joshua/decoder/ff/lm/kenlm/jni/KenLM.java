package joshua.decoder.ff.lm.kenlm.jni;

import joshua.decoder.ff.lm.NGramLanguageModel;

import joshua.decoder.Support;
import java.util.List;

// TODO(Joshua devs): include my state object with your LM state then update this API to pass state instead of int[].  

public class KenLM implements NGramLanguageModel {
  static {
    System.loadLibrary("ken");
  }

  private final long pointer;
  private final int N;

  private final static native long construct(String file_name);
  private final static native void destroy(long ptr);
  private final static native int order(long ptr);

  private final static native void vocabAdd(long ptr, int index, String word);
  private final static native int vocabFindOrAdd(long ptr, String word);
  private final static native String vocabWord(long ptr, int index);

  private final static native float prob(long ptr, int words[]);
  private final static native float probString(long ptr, int words[], int start);

  public KenLM(String file_name) {
    pointer = construct(file_name);
    N = order(pointer);
  }

  public void destroy() {
    destroy(pointer);
  }

  public int getOrder() { return N; }

  public void vocabAdd(int index, String word) { vocabAdd(pointer, index, word); }
  public int vocabFindOrAdd(String word) { return vocabFindOrAdd(pointer, word); }
  public String vocabWord(int index) { return vocabWord(pointer, index); }

  public float prob(int words[]) { return prob(pointer, words); }
  public float probString(int words[], int start) { return probString(pointer, words, start); }

  /* implement NGramLanguageModel */
  /** @deprecated pass int arrays to prob instead.
   */
  @Deprecated
  public double sentenceLogProbability(List<Integer> sentence, int order, int startIndex) {
    return probString(Support.subIntArray(sentence, 0, sentence.size()), startIndex);
  }

  public double ngramLogProbability(int[] ngram, int order) {
    if (order != N && order != ngram.length) throw new RuntimeException("Lower order not supported.");
    return prob(ngram);
  }

  public double ngramLogProbability(int[] ngram) {
    return prob(ngram);
  }

  /** @deprecated pass int arrays to prob instead.
   */
  @Deprecated
  public double ngramLogProbability(List<Integer> ngram, int order) {
    return prob(Support.subIntArray(ngram, 0, ngram.size()));
  }

  // TODO(Joshua devs): fix the rest of your code to use LM state properly.  Then fix this.  
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
}
