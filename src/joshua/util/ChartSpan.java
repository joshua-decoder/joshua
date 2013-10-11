package joshua.util;

/**
 * CKY-based decoding makes extensive use of charts, which maintain information about spans (i, j)
 * over the length-n input sentence, 0 <= i <= j <= n. These charts are used for many things; for
 * example, lattices use a chart to denote whether there is a path between nodes i and j, and what
 * their costs is, and the decoder uses charts to record the partial application of rules (
 * {@link DotChart}) and the existence of proved items ({@link Chart}).
 * 
 * The dummy way to implement a chart is to initialize a two-dimensional array; however, this wastes
 * a lot of space, because the constraint (i <= j) means that only half of this space can ever be
 * used. This is especially a problem for lattices, where the sentence length (n) is the number of
 * nodes in the lattice!
 * 
 * Fortunately, there is a smarter way, since there is a simple deterministic mapping between chart
 * spans under a given maximum length. This class implements that in a generic way, introducing
 * large savings in both space and time.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class ChartSpan<Type> {
  Object[] chart;
  int max;

  public ChartSpan(int w, Type defaultValue) {
    this.max = w;

    /* offset(max,max) is the last position in the array */
    int size = offset(max, max);
    chart = new Object[size];

    /* Initialize all arcs to infinity, except self-loops, which have distance 0 */
    for (int i = 0; i < size; i++)
      chart[i] = defaultValue;
  }
  
  @SuppressWarnings("unchecked")
  public Type get(int i, int j) {
    return (Type) chart[offset(i, j)];
  }

  public void set(int i, int j, Type value) {
    chart[offset(i, j)] = value;
  }

  /**
   * This computes the offset into the one-dimensional array for a given span.
   * 
   * @param i
   * @param j
   * @return the offset
   * @throws InvalidSpanException
   */
  private int offset(int i, int j) {
    if (i < 0 || j > max || i > j) {
      System.err.println(String.format("* FATAL: Invalid span (%d,%d | %d)", i, j, max));
      System.exit(31);
    }

    return i * (max + 1) - i * (i + 1) / 2 + j;
  }

  /**
   * Convenience function for setting the values along the diagonal.
   * 
   * @param value
   */
  public void setDiagonal(Type value) {
    for (int i = 0; i < max; i++)
      set(i, i, value);
  }
}