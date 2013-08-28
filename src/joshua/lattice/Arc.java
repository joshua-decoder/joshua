package joshua.lattice;


/**
 * An arc in a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * 
 * @param Label Type of label associated with an arc.
 */
public class Arc<Label> {

  /**
   * Weight of this arc. Package-private scope so that Node and Lattice can quickly access this
   * variable.
   */
  // TODO should be a vector of costs
  private final float cost;

  /**
   * Node where this arc ends. Package-private scope so that Node and Lattice can quickly access
   * this variable.
   */
  private final Node<Label> head;

  /**
   * Node where this arc begins. Package-private scope so that Node and Lattice can quickly access
   * this variable.
   */
  private final Node<Label> tail;

  /**
   * Label associated with this arc. Package-private scope so that Node and Lattice can quickly
   * access this variable.
   */
  private final Label label;

  /**
   * Creates an arc with the specified head, tail, cost, and label.
   * 
   * @param head The node where this arc begins.
   * @param tail The node where this arc ends.
   * @param cost The cost of this arc.
   * @param label The label associated with this arc.
   */
  public Arc(Node<Label> tail, Node<Label> head, float cost, Label label) {
    this.tail = tail;
    this.head = head;
    this.cost = cost;
    this.label = label;
  }

  /**
   * Gets the cost of this arc.
   * 
   * @return The cost of this arc.
   */
  // TODO should support indexing for multiple costs associated with each arc
  public float getCost() {
    return cost;
  }

  /**
   * Gets the tail of this arc (the node where this arc begins).
   * 
   * @return The tail of this arc.
   */
  public Node<Label> getTail() {
    return tail;
  }

  /**
   * Gets the head of this arc (the node where this arc ends).
   * 
   * @return The head of this arc.
   */
  public Node<Label> getHead() {
    return head;
  }

  /**
   * Gets the label associated with this arc.
   * 
   * @return The label associated with this arc.
   */
  public Label getLabel() {
    return label;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    s.append(label.toString());
    s.append("  :  ");
    s.append(tail.toString());
    s.append(" ==> ");
    s.append(head.toString());
    s.append("  :  ");
    s.append(cost);

    return s.toString();
  }

}
