package joshua.lattice;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares nodes based only on the natural order of their integer identifiers.
 * 
 * @author Lane Schwartz
 */
public class NodeIdentifierComparator implements Comparator<Node<?>>, Serializable {

  private static final long serialVersionUID = 1L;

  /* See Javadoc for java.util.Comparator#compare */
  public int compare(Node<?> o1, Node<?> o2) {
    if (o1.id() < o2.id())
      return -1;
    else if (o1.id() == o2.id())
      return 0;
    return 1;
  }
}
