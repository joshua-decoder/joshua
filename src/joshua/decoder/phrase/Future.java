package joshua.decoder.phrase;

/**
 * This class represents the future cost of a hypothesis.
 * 
 * TODO: initial bound is not being set correctly	
 */

import joshua.util.ChartSpan;

public class Future {
  
  // Square matrix with half the values ignored.
  private ChartSpan<Float> entries;

  private int sentlen;
  
  public Future(PhraseChart chart) {

    sentlen = chart.SentenceLength();
    entries = new ChartSpan<Float>(sentlen, Float.NEGATIVE_INFINITY);

    for (int begin = 0; begin <= chart.SentenceLength(); begin++) {
      // Nothing is nothing (this is a useful concept when two phrases abut)
      SetEntry(begin, begin,  0.0f);
      // Insert phrases
      int max_end = Math.min(begin + chart.MaxSourcePhraseLength(), chart.SentenceLength());
      for (int end = begin + 1; end <= max_end; end++) {
        TargetPhrases phrases = chart.Range(begin, end);
        if (phrases != null) {
          // TODO: what's the cost?
//          SetEntry(begin, end, phrases.getVertex().Bound());
          System.err.println("Future::Future(): WARNING: initial bound not set correctly");
          SetEntry(begin, end, 0.0f);
        }
      }
    }
    
    // All the phrases are in, now do minimum dynamic programming.  Lengths 0 and 1 were already handled above.
    for (int length = 2; length <= chart.SentenceLength(); length++) {
      for (int begin = 0; begin <= chart.SentenceLength() - length; begin++) {
        for (int division = begin + 1; division < begin + length; division++) {
          SetEntry(begin, begin + length, Math.max(Entry(begin, begin + length), Entry(begin, division) + Entry(division, begin + length)));
        }
      }
    }
  }
  
  public float Full() {
    return Entry(0, sentlen);
  }

  // Calculate change in rest cost when the given coverage is to be covered.                       
  public float Change(Coverage coverage, int begin, int end) {
    int left = coverage.LeftOpen(begin);
//    int right = coverage.RightOpen(end, sentence_length_plus_1 - 1);
    int right = coverage.RightOpen(end, sentlen);
    return Entry(left, begin) + Entry(end, right) - Entry(left, right);
  }
  
  private float Entry(int begin, int end) {
    assert end >= begin;
    assert end < this.sentlen;
    return entries.get(begin, end);
  }
  
  private void SetEntry(int begin, int end, float value) { // &float Entry(begin, end)
    assert end >= begin;
    assert end < this.sentlen;
    entries.set(begin, end, value);
  }

}
