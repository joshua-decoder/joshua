package joshua.decoder.ff.lm;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;

/**
 * This class implements NGramLanguageModel by creating wrappers around the necessary functions to
 * capture common errors. Most methods are declared final, in an attempt to limit what subclasses
 * may be defined.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public abstract class AbstractLM extends DefaultNGramLanguageModel {

  public AbstractLM(int order) {
    super(order);
  }

  @Override
  public final float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    return super.sentenceLogProbability(sentence, order, startIndex);
  }

  @Override
  public final float ngramLogProbability(int[] ngram) {
    return super.ngramLogProbability(ngram);
  }

  @Override
  public final float ngramLogProbability(int[] ngram, int order) {
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
    if (probability < -JoshuaConfiguration.lm_ceiling_cost) {
      probability = -JoshuaConfiguration.lm_ceiling_cost;
    }
    return probability;
  }

  protected abstract float ngramLogProbability_helper(int[] ngram, int order);


  /**
   * @deprecated this function is much slower than the int[] version
   */
  @Override
  @Deprecated
  public final float logProbOfBackoffState(List<Integer> ngram, int order,
      int qtyAdditionalBackoffWeight) {
    return logProbabilityOfBackoffState(Support.subIntArray(ngram, 0, ngram.size()), order,
        qtyAdditionalBackoffWeight);
  }

  @Override
  public final float logProbabilityOfBackoffState(int[] ngram, int order,
      int qtyAdditionalBackoffWeight) {
    if (ngram.length > order) {
      throw new RuntimeException("ngram length is greather than the max order");
    }
    if (ngram[ngram.length - 1] != LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID) {
      throw new RuntimeException("last wrd is not <bow>");
    }
    if (qtyAdditionalBackoffWeight > 0) {
      return logProbabilityOfBackoffState_helper(ngram, order, qtyAdditionalBackoffWeight);
    } else {
      return 0;
    }
  }


  protected abstract float logProbabilityOfBackoffState_helper(int[] ngram, int order,
      int qtyAdditionalBackoffWeight);


  // BUG: We should have different classes based on the configuration in use
  public int[] leftEquivalentState(int[] originalState, int order, double[] cost) {
    if (JoshuaConfiguration.use_left_equivalent_state)
      throw new UnsupportedOperationException(
          "getLeftEquivalentState is not overwritten by a concrete class");

    return originalState;
  }


  // BUG: We should have different classes based on the configuration in use
  public int[] rightEquivalentState(int[] originalState, int order) {
    if (!JoshuaConfiguration.use_right_equivalent_state
        || originalState.length != this.ngramOrder - 1) {
      return originalState;
    } else {
      throw new UnsupportedOperationException(
          "getRightEquivalentState is not overwritten by a concrete class");
    }
  }
}
