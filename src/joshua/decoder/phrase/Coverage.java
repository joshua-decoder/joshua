package joshua.decoder.phrase;

import joshua.corpus.Span;

public class Coverage {
  private int firstZero;
  private long bits;
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    long mask = 1L;
    for (int i = 0; i < 20; i++) {
      sb.append((bits & mask) > 0 ? "x" : ".");
      mask <<= 1;
    }
    
    return sb.toString();
  }
  
  public Coverage() {
    firstZero = 0;
    bits = 0;
  }
  
  public Coverage(int fz, long bits) {
    this.firstZero = fz;
    this.bits = bits;
  }
  
  public void Set(int begin, int end) {
    assert compatible(begin, end);
    if (begin == firstZero) {
      firstZero = end;
      bits >>= (end - begin);
      while ((bits & 1) != 0) {
        ++firstZero;
        bits >>= 1;
      }
    } else {
      bits |= pattern(begin, end);
    }
    
  }
  
  public boolean compatible(int begin, int end) {
    return (begin >= firstZero) && ((pattern(begin, end) & bits) == 0L);
  }

  public int firstZero() {
    return firstZero;
  }
  
  public int LeftOpen(int begin) {
    for (int i = begin - firstZero; i > 0; --i) {
      if (((bits & (1L << i)) != 0)) {
        assert compatible(i + firstZero + 1, begin);
        assert ! compatible(i + firstZero, begin);
        return i + firstZero + 1;
      }
    }
    
    assert compatible(firstZero, begin);
    return firstZero;
  }
  
  public int RightOpen(int end, int sentenceLength) {
    for (int i = end - firstZero; i < Math.min(64, sentenceLength - firstZero); i++) {
      if ( (bits & (1L << i)) != 0) {
        return i + firstZero;
      }
    }
    return sentenceLength;
  }
  
  public long pattern(int begin, int end) {
    assert begin >= firstZero;
    assert end - firstZero < 64;
    return (1L << (end - firstZero)) - (1L << (begin - firstZero));
  }
  
  public long getCoverage() {
    return bits;
  }
  
  public Coverage or(Span span) {
    Coverage c = new Coverage(this.firstZero, this.bits);
    c.Set(span.start, span.end);
    return c;
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
    return (int)getCoverage() * firstZero();
  }
}
