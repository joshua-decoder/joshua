package joshua.decoder.phrase;

import java.util.BitSet;

import joshua.corpus.Span;

/**
 * Represents a coverage vector. The vector is relative to a hypothesis. {firstZero} denotes the
 * first uncovered word of the sentence, and {bits} contains the coverage vector of all the words
 * after it, with the first zero removed. 
 */

public class Coverage {
  
  // The index of the first uncovered word
  private int firstZero;

  // Bits with the first zero removed.                                                             
  // We also assume anything beyond this is zero due to the reordering window.                     
  // Lowest bits correspond to next word.    
  private BitSet bits;

  // Default bit vector length
  private static int INITIAL_LENGTH = 10;

  public Coverage() {
    firstZero = 0;
    bits = new BitSet(INITIAL_LENGTH);
  }
  
  public Coverage(int firstZero) {
    this.firstZero = firstZero;
    bits = new BitSet(INITIAL_LENGTH);
  }
  
  /**
   * Pretty-prints the coverage vector, making a guess about the length
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%d ", firstZero));

    for (int i = 0; i < Math.max(INITIAL_LENGTH, bits.length()); i++) { // only display first 10 bits
      sb.append(bits.get(i) ? "x" : ".");
    }

    return sb.toString();
  }

  /**
   * Initialize a coverage vector from another Coverage vector, creating a separate object.
   * 
   * @param firstZero
   * @param bits
   */
  public Coverage(Coverage other) {
    this.firstZero = other.firstZero;
    this.bits = (BitSet) other.bits.clone();
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
      bits = bits.get(end - begin, Math.max(end - begin, bits.length()));
      int firstClear = bits.nextClearBit(0);
      if (firstClear != 0) {
        // We might have exactly covered a gap, in which case we need to adjust shift
        // firstZero and the bits until we reach the new end
        firstZero += firstClear;
        bits = bits.get(firstClear,  bits.length());
      }
    } else {
      // Set the bits relative to the currenS
      bits.or(pattern(begin, end));
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
   * Tests whether a new range is compatible with the current coverage vector. It must be after
   * the first uncovered word, obviously, and must not conflict with spans after the first
   * uncovered word.
   * 
   * @param begin the begin index (absolute)
   * @param end the end index (absolute)
   * @return true if the span is compatible with the coverage vector
   */
  public boolean compatible(int begin, int end) {
    if (begin >= firstZero) {
      BitSet pattern = new BitSet();
      pattern.set(begin - firstZero, end - firstZero);
      return ! bits.intersects(pattern);
    }
    return false;
  }
  
  /**
   * Returns the source sentence index of the first uncovered word.
   * 
   * @return the index
   */
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
      if (bits.get(i)) {
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
      if (bits.get(i)) {
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
  public BitSet pattern(int begin, int end) {
//    System.err.println(String.format("pattern(%d,%d) %d %s %s", begin, end, firstZero, begin >= firstZero, toString()));
    assert begin >= firstZero;
    BitSet pattern = new BitSet(INITIAL_LENGTH);
    pattern.set(begin - firstZero, end - firstZero);
    return pattern;
  }

  /**
   * Returns the underlying coverage bits.
   * 
   * @return
   */
  public BitSet getCoverage() {
    return bits;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Coverage) {
      Coverage other = (Coverage) obj;
      return getCoverage().equals(other.getCoverage()) && firstZero() == other.firstZero();
    }

    return false;
  }

  @Override
  public int hashCode() {
    return getCoverage().hashCode() * firstZero();
  }
}
