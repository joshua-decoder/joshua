package joshua.decoder.phrase;

/***
 * This class represents the future cost of a hypothesis. The future cost of a hypothesis is the
 * cost of covering all uncovered words. The way this is computed is with a simple dynamic program
 * that computes, for each span of the input, the best possible way to cover that span with
 * phrases from the phrase table. No non-local features (e.g., the language model cost) are used
 * in computing this estimate.	
 */

import joshua.decoder.Decoder;
import joshua.util.ChartSpan;

public class Future {
  
  // Square matrix with half the values ignored.
  private ChartSpan<Float> entries;

  private int sentlen;
  
  /**
   * Computes bottom-up the best way to cover all spans of the input sentence, using the phrases
   * that have been assembled in a {@link PhraseChart}. Requires that there be a translation at least
   * for every word (which can be accomplished with a pass-through grammar).
   * 
   * @param chart
   */
  public Future(PhraseChart chart) {

    sentlen = chart.SentenceLength();
    entries = new ChartSpan<Float>(sentlen + 1, Float.NEGATIVE_INFINITY);

    /*
     * The sentence is represented as a sequence of words, with the first and last words set
     * to <s> and </s>. We start indexing at 1 because the first word (<s>) is always covered.
     */
    for (int begin = 1; begin <= chart.SentenceLength(); begin++) {
      // Nothing is nothing (this is a useful concept when two phrases abut)
      setEntry(begin, begin,  0.0f);
      // Insert phrases
      int max_end = Math.min(begin + chart.MaxSourcePhraseLength(), chart.SentenceLength());
      for (int end = begin + 1; end <= max_end; end++) {
        
        // Moses doesn't include the cost of applying </s>, so force it to zero
        if (begin == sentlen - 1 && end == sentlen) 
          setEntry(begin, end, 0.0f);
        else {
          TargetPhrases phrases = chart.getRange(begin, end);
          if (phrases != null)
            setEntry(begin, end, phrases.get(0).getEstimatedCost());
        }
      }
    }
    
    // All the phrases are in, now do minimum dynamic programming.  Lengths 0 and 1 were already handled above.
    for (int length = 2; length <= chart.SentenceLength(); length++) {
      for (int begin = 1; begin <= chart.SentenceLength() - length; begin++) {
        for (int division = begin + 1; division < begin + length; division++) {
          setEntry(begin, begin + length, Math.max(getEntry(begin, begin + length), getEntry(begin, division) + getEntry(division, begin + length)));
        }
      }
    }
    
    if (Decoder.VERBOSE >= 3) {
      for (int i = 1; i < chart.SentenceLength(); i++)
        for (int j = i + 1; j < chart.SentenceLength(); j++)
          System.err.println(String.format("future cost from %d to %d is %.3f", i-1, j-2, getEntry(i, j)));
    }
  }
  
  public float Full() {
//    System.err.println("Future::Full(): " + Entry(1, sentlen));
    return getEntry(1, sentlen);
  }

  /**
   * Calculate change in rest cost when the given coverage is to be covered.
   */                       
  public float Change(Coverage coverage, int begin, int end) {
    int left = coverage.leftOpening(begin);
    int right = coverage.rightOpening(end, sentlen);
//    System.err.println(String.format("Future::Change(%s, %d, %d) left %d right %d %.3f %.3f %.3f", coverage, begin, end, left, right,
//        Entry(left, begin), Entry(end, right), Entry(left, right)));
    return getEntry(left, begin) + getEntry(end, right) - getEntry(left, right);
  }
  
  private float getEntry(int begin, int end) {
    assert end >= begin;
    assert end < this.sentlen;
    return entries.get(begin, end);
  }
  
  private void setEntry(int begin, int end, float value) {
    assert end >= begin;
    assert end < this.sentlen;
//    System.err.println(String.format("future cost from %d to %d is %.5f", begin, end, value));
    entries.set(begin, end, value);
  }
}
