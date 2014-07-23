package joshua.util;

import java.util.AbstractList;
import java.util.List;

/**
 * List that performs sampling at specified intervals.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SampledList<E> extends AbstractList<E> implements List<E> {

  private final List<E> list;
  private final int size;
  private final int stepSize;

  /**
   * Constructs a sampled list backed by a provided list.
   * <p>
   * The maximum size of this list will be no greater than the provided sample size.
   * 
   * @param list List from which to sample.
   * @param sampleSize Maximum number of items to include in the new sampled list.
   */
  public SampledList(List<E> list, int sampleSize) {
    this.list = list;

    int listSize = list.size();

    if (listSize <= sampleSize) {
      this.size = listSize;
      this.stepSize = 1;
    } else {
      this.size = sampleSize;
      this.stepSize = listSize / sampleSize;
    }

  }

  @Override
  public E get(int index) {
    return list.get(index * stepSize);
  }

  @Override
  public int size() {
    return size;
  }

}
