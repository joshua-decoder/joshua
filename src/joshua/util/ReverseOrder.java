package joshua.util;

import java.util.Comparator;

/**
 * ReverseOrder is a Comparator that reverses the natural order of Comparable objects.
 * 
 * @author Chris Callison-Burch
 * @since 2 June 2008
 */
public class ReverseOrder<K extends Comparable<K>> implements Comparator<K> {

  public int compare(K obj1, K obj2) {
    int comparison = obj1.compareTo(obj2);
    if (comparison != 0) {
      comparison = comparison * -1;
    }
    return comparison;
  }

}
