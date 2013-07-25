package joshua.ui.tree_visualizer;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import joshua.ui.tree_visualizer.tree.Tree;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class DerivationTree extends DirectedOrderedSparseMultigraph<Node, DerivationTreeEdge> {
  /**
   * Eclipse thinks this is necessary.
   */
  private static final long serialVersionUID = 2914449263979566324L;

  public final Node root;
  public final Node sourceRoot;

  public DerivationTree(Tree t, String source) {
    final Tree.Node treeRoot = t.root();
    final String rootLabel = treeRoot.label();
    root = new Node(rootLabel, false);
    sourceRoot = new Node(rootLabel, true);
    addVertex(root);
    addVertex(sourceRoot);
    addSubtreeRootedAt(root, treeRoot);
    final String[] sourceWords = source.split("\\s+");
    addSourceSubtreeRootedAt(sourceRoot, treeRoot, 0, sourceWords.length, sourceWords);
  }

  private void addSubtreeRootedAt(Node n, Tree.Node tn) {
    for (Tree.Node child : tn.children()) {
      Node childNode = new Node(child.label(), false);
      addVertex(childNode);
      addEdge(new DerivationTreeEdge(false), new Pair(n, childNode), EdgeType.DIRECTED);
      addSubtreeRootedAt(childNode, child);
    }
  }

  private void addSourceSubtreeRootedAt(Node n, Tree.Node tn, int firstIndex, int lastIndex,
      String[] sourceWords) {
    int nextUncoveredIndex = firstIndex;
    Tree.NodeSourceStartComparator cmp = new Tree.NodeSourceStartComparator();
    List<Tree.Node> children = tn.children();
    Collections.sort(children, cmp);
    for (Tree.Node child : children) {
      if (child.isLeaf()) {
        continue;
      }
      int sourceStartIndex = child.sourceStartIndex();
      int sourceEndIndex = child.sourceEndIndex();
      if (sourceStartIndex > nextUncoveredIndex) {
        insertSourceLeaf(n, sourceWords, nextUncoveredIndex, sourceStartIndex);
      }
      Node childNode = new Node(child.label(), true);
      addEdge(new DerivationTreeEdge(true), new Pair(n, childNode), EdgeType.DIRECTED);
      nextUncoveredIndex = sourceEndIndex;
      addSourceSubtreeRootedAt(childNode, child, sourceStartIndex, sourceEndIndex, sourceWords);
    }
    if (nextUncoveredIndex < lastIndex) {
      insertSourceLeaf(n, sourceWords, nextUncoveredIndex, lastIndex);
    }
  }

  private void insertSourceLeaf(Node n, String[] words, int start, int end) {
    final String[] leafWords = Arrays.copyOfRange(words, start, end);
    String label = leafWords[0];
    for (int i = 1; i < leafWords.length; i++) {
      label += " " + leafWords[i];
    }
    Node childNode = new Node(label, true);
    addEdge(new DerivationTreeEdge(true), new Pair(n, childNode), EdgeType.DIRECTED);
  }

  public void setSubtreeHighlight(Node n, boolean b) {
    n.isHighlighted = b;
    for (Node s : getSuccessors(n)) {
      setSubtreeHighlight(s, b);
    }
    return;
  }
}
