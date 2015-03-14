package joshua.decoder.phrase;

import joshua.corpus.Span;

/**
 * Represents a coverage vector. The vector is relative to a hypothesis. {firstZero} denotes the
 * first uncovered word of the sentence, and {bits} contains the coverage vector of all the words
 * after it, with the first zero removed. Since {bits} is 64 bits, this means we have a hard-coded
 * rule that reordering distance + max phrase length <= 64.
 */

public class Coverage {
  
  // The index of the first uncovered word
  private int firstZero;

  // Bits with the first zero removed.                                                             
  // We also assume anything beyond this is zero due to the reordering window.                     
  // Lowest bits correspond to next word.    
  private long bits;

  /**
   * Pretty-prints the coverage vector, making a guess about the length
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%d ", firstZero));

    long mask = 1L;
    for (int i = 0; i < 10; i++) { // only display first 10 bits
      sb.append((bits & mask) > 0 ? "x" : ".");
      mask <<= 1;
    }

    return sb.toString();
  }

  public Coverage() {
    firstZero = 0;
    bits = 0;
  }
  
  public Coverage(int firstZero) {
    this.firstZero = firstZero;
    bits = 0;
  }

  /**
   * Initialize a coverage vector from another Coverage vector.
   * 
   * @param firstZero
   * @param bits
   */
  public Coverage(Coverage other) {
    this.firstZero = other.firstZero;
    this.bits = other.bits;
  }

  /**
   * Turns on all bits from position start to position (end - 1), that is, in the range [start .. end).
   * This is done relative to the current coverage vector, of course, which may not start at 0.
   * 
   * @param begin
   * @param end
   */
  public void set(int begin, int end) {
    assert compatible(begin, end);

//    StringBuffer sb = new StringBuffer();
//    sb.append(String.format("SET(%d,%d) %s", begin, end, this));
    
    if (begin == firstZero) {
      // A concatenation. 
      firstZero = end;
      bits >>= (end - begin);
      while ((bits & 1) != 0) {
        // We might have exactly covered a gap, in which case we need to adjust shift
        // firstZero and the bits until we reach the new end
        ++firstZero;
        bits >>= 1;
      }
    } else {
      bits |= pattern(begin, end);
    }

//    sb.append(String.format(" -> %s", this));
//    System.err.println(sb);
  }
  
  /**
   * Convenience function.
   */
  public final void set(Span span) {
    set(span.start, span.end);
  }

  /**
   * A span is compatible with the current coverage vector if it begins at or after the first zero
   * and there is no overlap with currently covered bits. Recall that {firstZero} is an absolute
   * index marking where in the input the bit vector begins, while {bits} is the coverage vector
   * for positions [{firstZero+1}..{firstZero+64+1})
   * 
   * @param begin the begin index (absolute)
   * @param end the end index (absolute)
   * @return true if the span is compatible with the coverage vector
   */
  public boolean compatible(int begin, int end) {
    return (begin >= firstZero) && ((pattern(begin, end) & bits) == 0L);
  }

  public int firstZero() {
    return firstZero;
  }

  /**
   * LeftOpen() and RightOpen() find the larger gap in which a new source phrase pair sits.
   * When using a phrase pair covering (begin, end), the pair
   * 
   *     (LeftOpen(begin), RightOpen(end, sentence_length))  
   *     
   * provides this gap.                                           

   * Find the left bound of the gap in which the phrase [begin, ...) sits.                         
   * 
   * @param begin the start index of the phrase being applied.
   * @return
   */
  public int leftOpening(int begin) {
    for (int i = begin - firstZero; i > 0; --i) {
      if (((bits & (1L << i)) != 0)) {
        assert compatible(i + firstZero + 1, begin);
        assert !compatible(i + firstZero, begin);
        return i + firstZero + 1;
      }
    }

    assert compatible(firstZero, begin);
    return firstZero;
  }

  /**
   * LeftOpen() and RightOpen() find the larger gap in which a new source phrase pair sits.
   * When using a phrase pair covering (begin, end), the pair
   * 
   *     (LeftOpen(begin), RightOpen(end, sentence_length))  
   *     
   * provides this gap.                                           
   * 
   * Finds the right bound of the enclosing gap, or the end of sentence, whichever is less.
   */
  public int rightOpening(int end, int sentenceLength) {
    for (int i = end - firstZero; i < Math.min(64, sentenceLength - firstZero); i++) {
      if ((bits & (1L << i)) != 0) {
        return i + firstZero;
      }
    }
    return sentenceLength;
  }

  /**
   * Creates a bit vector with the same offset as the current coverage vector, flipping on
   * bits begin..end.
   * 
   * @param begin the begin index (absolute)
   * @param end the end index (absolute)
   * @return a bit vector (relative) with positions [begin..end) on
   */
  public long pattern(int begin, int end) {
    assert begin >= firstZero;
    assert end - firstZero < 64;
    return (1L << (end - firstZero)) - (1L << (begin - firstZero));
  }

  public long getCoverage() {
    return bits;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Coverage) {
      Coverage other = (Coverage) obj;
      return getCoverage() == other.getCoverage() && firstZero() == other.firstZero();
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (int) getCoverage() * firstZero();
  }
  
  public static void main(String[] args) {
    
  }
}
