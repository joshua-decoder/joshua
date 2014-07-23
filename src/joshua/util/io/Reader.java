package joshua.util.io;

import java.io.IOException;
import java.util.Iterator;

/**
 * Common interface for Reader type objects.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public interface Reader<E> extends Iterable<E>, Iterator<E> {

  /** Close the reader, freeing all resources. */
  void close() throws IOException;

  /** Determine if the reader is ready to read a line. */
  boolean ready() throws IOException;

  /** Read a "line" and return an object representing it. */
  E readLine() throws IOException;
}
