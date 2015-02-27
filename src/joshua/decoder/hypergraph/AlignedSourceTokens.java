package joshua.decoder.hypergraph;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Class that represents a one to (possibly) many alignment from target to
 * source. Extends from a LinkedList. Instances of this class are updated by the
 * WordAlignmentExtractor.substitute() method. The <shiftBy> method shifts the
 * elements in the list by a scalar to reflect substitutions of non terminals in
 * the rule. if indexes are final, i.e. the point instance has been substituted
 * into a parent WordAlignmentState once, <isFinal> is set to true. This is
 * necessary since the final source index of a point is known once we have
 * substituted in a complete WordAlignmentState into its parent. If the index in
 * the list is a non terminal, <isNonTerminal> = true
 */
class AlignedSourceTokens extends LinkedList<Integer> {

  private static final long serialVersionUID = 1L;
  /** whether this Point refers to a non terminal in source&target */
  private boolean isNonTerminal = false;
  /** whether this instance does not need to be updated anymore */
  private boolean isFinal = false;
  /** whether the word this Point corresponds to has no alignment in source */
  private boolean isNull = false;

  AlignedSourceTokens() {
  }

  void setFinal() {
    isFinal = true;
  }

  void setNonTerminal() {
    isNonTerminal = true;
  }

  void setNull() {
    isNull = true;
  }

  @Override
  /**
   * returns true if element was added.
   */
  public boolean add(Integer x) {
    if (isNull || isNonTerminal)
      return false;
    return super.add(x);
  }

  public boolean isNonTerminal() {
    return isNonTerminal;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isNull() {
    return isNull;
  }

  /**
   * shifts each item in the LinkedList by <shift>.
   * Only applies to items larger than <start>
   */
  void shiftBy(int start, int shift) {
    if (!isFinal && !isNull) {
      ListIterator<Integer> it = this.listIterator();
      while (it.hasNext()) {
        int x = it.next();
        if (x > start) {
          it.set(x + shift);
        }
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (isFinal)
      sb.append("f");
    if (isNull) {
      sb.append("[NULL]");
    } else {
      sb.append(super.toString());
    }
    if (isNonTerminal)
      sb.append("^");
    return sb.toString();
  }
}