package joshua.decoder.phrase;

/***
 * This class represents the future cost of a hypothesis. The future cost of a hypothesis is the
 * cost of covering all uncovered words. The way this is computed is with a simple dynamic program
 * that computes, for each span of the input, the best possible way to cover that span with
 * phrases from the phrase table. No non-local features (e.g., the language model cost) are used
 * in computing this estimate.	
 */

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
      SetEntry(begin, begin,  0.0f);
      // Insert phrases
      int max_end = Math.min(begin + chart.MaxSourcePhraseLength(), chart.SentenceLength());
      for (int end = begin + 1; end <= max_end; end++) {
        TargetPhrases phrases = chart.Range(begin, end);
        if (phrases != null) {
          // TODO: what's the cost?
//          SetEntry(begin, end, phrases.getVertex().Bound());
            System.err.println(String.format("  Found %d over (%d,%d)", phrases.size(), begin, end));
          SetEntry(begin, end, phrases.get(0).getEstimatedCost());
        }
      }
    }
    
    // All the phrases are in, now do minimum dynamic programming.  Lengths 0 and 1 were already handled above.
    for (int length = 2; length <= chart.SentenceLength(); length++) {
      for (int begin = 1; begin <= chart.SentenceLength() - length; begin++) {
        for (int division = begin + 1; division < begin + length; division++) {
          SetEntry(begin, begin + length, Math.max(Entry(begin, begin + length), Entry(begin, division) + Entry(division, begin + length)));
        }
      }
    }
  }
  
  public float Full() {
    System.err.println("Future::Full(): " + Entry(1, sentlen));
    return Entry(1, sentlen);
  }

  /**
   * Calculate change in rest cost when the given coverage is to be covered.
   */                       
  public float Change(Coverage coverage, int begin, int end) {
    int left = coverage.LeftOpen(begin);
    int right = coverage.RightOpen(end, sentlen);
//    System.err.println(String.format("Future::Change(%s, %d, %d) left %d right %d %.3f %.3f %.3f", coverage, begin, end, left, right,
//        Entry(left, begin), Entry(end, right), Entry(left, right)));
    return Entry(left, begin) + Entry(end, right) - Entry(left, right);
  }
  
  private float Entry(int begin, int end) {
    assert end >= begin;
    assert end < this.sentlen;
    return entries.get(begin, end);
  }
  
  private void SetEntry(int begin, int end, float value) {
    assert end >= begin;
    assert end < this.sentlen;
//    if (value > Float.NEGATIVE_INFINITY)
//      System.err.println(String.format("Future::SetEntry(%d,%d,%.5f)", begin, end, value));
    entries.set(begin, end, value);
  }

}
