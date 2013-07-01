package joshua.decoder.ff.lm.kenlm.jni;

import java.util.List;

import joshua.decoder.ff.lm.NGramLanguageModel;

// TODO(Joshua devs): include my state object with your LM state then
// update this API to pass state instead of int[].

public class KenLM implements NGramLanguageModel {

  static {
    System.loadLibrary("ken");
  }

  private final long pointer;
  // this is read from the config file, used to set maximum order
  private final int ngramOrder;
  // inferred from model file (may be larger than ngramOrder)
  private final int N;

  private final static native long construct(String file_name);

  private final static native void destroy(long ptr);

  private final static native int order(long ptr);

  private final static native boolean registerWord(long ptr, String word, int id);

  private final static native float prob(long ptr, int words[]);

  private final static native float probString(long ptr, int words[], int start);

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

  // Apparently Zhifei starts some array indices at 1. Change to
  // 0-indexing.
  public float probString(int words[], int start) {
    return probString(pointer, words, start - 1);
  }

  @Override
  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    return probString(sentence, startIndex);
  }

  public float ngramLogProbability(int[] ngram, int order) {
    if (order != N && order != ngram.length)
      throw new RuntimeException("Lower order not supported.");
    return prob(ngram);
  }

  public float ngramLogProbability(int[] ngram) {
    return prob(ngram);
  }

  // TODO(Joshua devs): fix the rest of your code to use LM state properly.
  // Then fix this.
  public float logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0;
  }

  public float logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) {
    return 0;
  }

  public int[] leftEquivalentState(int[] originalState, int order, double[] cost) {
    return originalState;
  }

  public int[] rightEquivalentState(int[] originalState, int order) {
    return originalState;
  }
}
