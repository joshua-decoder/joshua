package joshua.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * This class provides a null-object Iterator. That is, an iterator over an empty collection.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class NullIterator<E> implements Iterable<E>, Iterator<E> {

  // ===============================================================
  // Iterable -- for foreach loops, because sometimes Java can be very stupid
  // ===============================================================

  /**
   * Return self as an iterator. We restrict the return type because some code is written to accept
   * both Iterable and Iterator, and the fact that we are both confuses Java. So this is just an
   * upcast, but more succinct to type.
   */
  public Iterator<E> iterator() {
    return this;
  }


  // ===============================================================
  // Iterator
  // ===============================================================

  /** Always returns false. */
  public boolean hasNext() {
    return false;
  }

  /** Always throws {@link NoSuchElementException}. */
  public E next() throws NoSuchElementException {
    throw new NoSuchElementException();
  }

  /** Unsupported. */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
