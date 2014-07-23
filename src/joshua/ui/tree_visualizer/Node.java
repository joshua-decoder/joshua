package joshua.ui.tree_visualizer;

/**
 * A representation of a node in a derivation tree. The derivation tree class itself is
 * parameterized in terms of this class and the <code>DerivationEdge</code> class. A
 * <code>Node</code> may represent either a non-terminal symbol or one or more terminal symbols of
 * the derivation.
 */
public class Node {
  /**
   * The label to be shown on the node. If the node is a non-terminal symbol, it is the name of the
   * symbol. Otherwise, it is terminal symbols joined with spaces.
   */
  public final String label;

  /**
   * Indicates whether this node is part of the source-side of target- side derivation tree.
   */
  public final boolean isSource;

  /**
   * A boolean to let the renderer know whether this vertex is highlighted.
   */
  public boolean isHighlighted = false;

  /**
   * Constructor used for root nodes or nodes whose parent is not given.
   * 
   * @param label a <code>String</code> that represents the symbols at this node
   * @param isSource a boolean saying whether this is a source-side node
   */
  public Node(String label, boolean isSource) {
    this.label = label;
    this.isSource = isSource;
  }

	@Override
  public String toString() {
    return label;
  }
}
