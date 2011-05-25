package joshua.decoder.ff.lm.kenlm.jni;

import joshua.decoder.ff.lm.NGramLanguageModel;

import joshua.decoder.Support;
import java.util.List;

// TODO(Joshua devs): include my state object with your LM state then update this API to pass state instead of int[].  

public abstract class KenLM implements NGramLanguageModel {
  static {
    System.loadLibrary("ken");
  }

  protected final long pointer;
  protected final int N;

  private final static native int classify(String file_name);

  static public KenLM Load(String file_name, VocabCallback vocab) {
    if (classify(file_name) == 2) {
      return new KenTrie(file_name, vocab);
    } else {
      return new KenProbing(file_name, vocab);
    }
  }

  protected KenLM(long point) {
    pointer = point;
    N = internalOrder();
  }

  /* API */
  abstract protected int internalOrder();

  public final int getOrder() { return N; }

  abstract public int vocab(String word);

  abstract public float prob(int[] words);

  abstract float probString(int[] words, int start);

  abstract public void destroy();

  /* implement NGramLanguageModel */
  /** @deprecated pass int arrays to prob instead.
   */
  @Deprecated
  public double sentenceLogProbability(List<Integer> sentence, int order, int startIndex) {
    return probString(Support.subIntArray(sentence, 0, sentence.size()), startIndex);
  }

  public double ngramLogProbability(int[] ngram, int order) {
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
