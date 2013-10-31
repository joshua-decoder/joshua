package joshua.lattice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A node in a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * 
 * @param <Label> Type of label associated with an arc.
 */
public class Node<Label> {

  // ===============================================================
  // Member variables
  // ===============================================================

  /**
   * Numeric integer identifier of this node. Package-private scope so that Lattice can quickly
   * access this variable.
   */
  private Integer id;

  /**
   * Arcs which begin at this node. Package-private scope so that Lattice can quickly access this
   * variable.
   */
  private List<Arc<Label>> outgoingArcs;


  // ===============================================================
  // Constructor(s)
  // ===============================================================

  /**
   * Constructs a new node with the specified numeric identifier.
   */
  public Node(int id) {
    this.id = id;
    this.outgoingArcs = new ArrayList<Arc<Label>>();
  }


  // ===========================================================
  // Accessor methods (set/get)
  // ===========================================================

  /**
   * Gets the numeric integer identifier of this node.
   * 
   * @return Numeric integer identifier of this node.
   */
  public int getNumber() {
    return id;
    
  }
  
  public int id() {
    return id;
  }
  
  public void setID(int i) {
    this.id = i;
  }

  /**
   * Gets the arcs that begin at this node.
   * 
   * @return The arcs that begin at this node.
   */
  public List<Arc<Label>> getOutgoingArcs() {
    return outgoingArcs;
  }

  public void setOutgoingArcs(List<Arc<Label>> arcs) {
    outgoingArcs = arcs;
  }

  /**
   * Gets an iterable object capable of iterating over all nodes directly reachable from this node.
   * This will be all nodes which are the target of an outgoing arc from this node.
   * 
   * @return An iterable object capable of iterating over all nodes directly reachable from this
   *         node.
   */
  public Iterable<Node<Label>> reachableNodes() {
    final Iterator<Arc<Label>> arcIterator = outgoingArcs.iterator();

    return new Iterable<Node<Label>>() {
      public Iterator<Node<Label>> iterator() {
        return new Iterator<Node<Label>>() {

          public boolean hasNext() {
            return arcIterator.hasNext();
          }

          public Node<Label> next() {
            return arcIterator.next().getHead();
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }


  /**
   * Adds a new outgoing arc to this node that points to the specified destination. The new arc will
   * have the specified weight and specified label.
   * 
   * @param destination Destination node of the new outgoing arc.
   * @param weight Weight of the new outgoing arc.
   * @param label Label of the new outgoing arc.
   */
  public void addArc(Node<Label> destination, float weight, Label label) {
    outgoingArcs.add(new Arc<Label>(this, destination, weight, label));
  }


  /**
   * Gets the number of outgoing arcs that begin at this node.
   * 
   * @return The number of outgoing arcs that begin at this node.
   */
  public int size() {
    return outgoingArcs.size();
  }

  @Override
  public String toString() {
    return "Node-" + id;
  }

}
