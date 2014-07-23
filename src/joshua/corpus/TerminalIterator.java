package joshua.corpus;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator capable of iterating over those word identifiers in a phrase which represent terminals.
 * <p>
 * <em>Note</em>: This class is <em>not</em> thread-safe.
 * 
 * @author Lane Schwartz
 */
public class TerminalIterator implements Iterator<Integer> {

  private final int[] words;

  private int nextIndex = -1;
  private int next = Integer.MIN_VALUE;
  private boolean dirty = true;

  /**
   * Constructs an iterator for the terminals in the given list of words.
   * 
   * @param vocab
   * @param words
   */
  public TerminalIterator(int[] words) {
    this.words = words;
  }

  /* See Javadoc for java.util.Iterator#next(). */
  public boolean hasNext() {

    while (dirty || Vocabulary.nt(next)) {
      nextIndex++;
      if (nextIndex < words.length) {
        next = words[nextIndex];
        dirty = false;
      } else {
        return false;
      }
    }

    return true;
  }

  /* See Javadoc for java.util.Iterator#next(). */
  public Integer next() {
    if (hasNext()) {
      dirty = true;
      return next;
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * Unsupported operation, guaranteed to throw an UnsupportedOperationException.
   * 
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
