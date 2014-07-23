package joshua.util.io;

import java.io.IOException;

import joshua.util.NullIterator;


/**
 * This class provides a null-object Reader. This is primarily useful for when you may or may not
 * have a {@link Reader}, and you don't want to check for null all the time. All operations are
 * no-ops.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class NullReader<E> extends NullIterator<E> implements Reader<E> {

  // ===============================================================
  // Constructors and destructors
  // ===============================================================

  // TODO: use static factory method and singleton?
  public NullReader() {}

  /** A no-op. */
  public void close() throws IOException {}


  // ===============================================================
  // Reader
  // ===============================================================

  /**
   * Always returns true. Is this correct? What are the semantics of ready()? We're always capable
   * of delivering nothing, but we're never capable of delivering anything...
   */
  public boolean ready() {
    return true;
  }

  /** Always returns null. */
  public E readLine() throws IOException {
    return null;
  }
}
