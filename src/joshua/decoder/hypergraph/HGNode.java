package joshua.decoder.hypergraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.Prunable;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;

/**
 * this class implement Hypergraph node (i.e., HGNode); also known as Item in parsing.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Juri Ganitkevitch, <juri@cs.jhu.edu>
 */

// TODO: handle the case that the Hypergraph only maintains the one-best tree
@SuppressWarnings("rawtypes")
public class HGNode implements Prunable<HGNode> {

  public int i, j;

  // this is the symbol like: NP, VP, and so on
  public int lhs;

  // each hyperedge is an "and" node
  public List<HyperEdge> hyperedges = null;

  // used in pruning, compute_item, and transit_to_goal
  public HyperEdge bestHyperedge = null;

  // the key is the state id; remember the state required by each model, for example, edge-ngrams
  // for LM model
  TreeMap<StateComputer, DPState> dpStates;

  private Signature signature = null;

  // For pruning purposes.
  public boolean isDead = false;
  private double estTotalLogP = 0.0; // it includes the estimated LogP

  // ===============================================================
  // Constructors
  // ===============================================================

  public HGNode(int i, int j, int lhs, TreeMap<StateComputer, DPState> dpStates,
      HyperEdge hyperEdge, double pruningEstimate) {
    this.lhs = lhs;
    this.i = i;
    this.j = j;
    this.dpStates = dpStates;
    this.estTotalLogP = pruningEstimate;
    addHyperedgeInNode(hyperEdge);
  }

  // used by disk hg
  public HGNode(int i, int j, int lhs, List<HyperEdge> hyperedges, HyperEdge bestHyperedge,
      TreeMap<StateComputer, DPState> states) {
    this.i = i;
    this.j = j;
    this.lhs = lhs;
    this.hyperedges = hyperedges;
    this.bestHyperedge = bestHyperedge;
    this.dpStates = states;
  }

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * Adds the hyperedge to the list of incoming hyperedges (i.e., ways to form this node), creating
   * the list if necessary. We then update the cache of the best incoming hyperedge via a call to
   * the (obscurely named) semiringPlus().
   */
  public void addHyperedgeInNode(HyperEdge hyperEdge) {
    if (hyperEdge != null) {
      if (null == hyperedges)
        hyperedges = new ArrayList<HyperEdge>();
      hyperedges.add(hyperEdge);
      // Update the cache of this node's best incoming edge.
      semiringPlus(hyperEdge);
    }
  }

  /**
   * Convenience function to add a list of hyperedges one at a time.
   */
  public void addHyperedgesInNode(List<HyperEdge> hyperedges) {
    for (HyperEdge hyperEdge : hyperedges)
      addHyperedgeInNode(hyperEdge);
  }

  /**
   * Updates the cache of the best incoming hyperedge.
   */
  public void semiringPlus(HyperEdge hyperEdge) {
    if (null == bestHyperedge || bestHyperedge.bestDerivationLogP < hyperEdge.bestDerivationLogP) {
      bestHyperedge = hyperEdge;
    }
  }

  public TreeMap<StateComputer, DPState> getDPStates() {
    return dpStates;
  }

  public DPState getDPState(StateComputer state) {
    if (null == this.dpStates) {
      return null;
    } else {
      return this.dpStates.get(state);
    }
  }

  public void printInfo(Level level) {
    if (HyperGraph.logger.isLoggable(level))
      HyperGraph.logger.log(level,
          String.format("lhs: %s; logP: %.3f", lhs, bestHyperedge.bestDerivationLogP));
  }

  public Signature signature() {
    if (signature == null)
      signature = new Signature();
    return signature;
  }

  public class Signature {
    // Cached hash code.
    private int hash = 0;

    @Override
    public int hashCode() {
      if (hash == 0) {
        hash = 31 * lhs;
        // BUG: This is unsafe, because no guarantees of order are made between state types.
        // Iterate over all the node's states, hashing.
        if (null != dpStates && dpStates.size() > 0)
          for (DPState dps : dpStates.values())
            hash = hash * 19 + dps.hashCode();
      }
      return hash;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Signature) {
        HGNode that = ((Signature) other).node();
        if (lhs != that.lhs)
          return false;
        if (dpStates == null)
          return (that.dpStates == null);
        if (dpStates == null)
          return false;
        if (dpStates.size() != that.dpStates.size())
          return false;
        for (StateComputer sc : dpStates.keySet()) {
          if (!dpStates.get(sc).equals(that.dpStates.get(sc)))
            return false;
        }
        return true;
      }
      return false;
    }

    public HGNode node() {
      return HGNode.this;
    }
  }

  public double getEstTotalLogP() {
    return this.estTotalLogP;
  }

  /*
   * this will called by the sorting in Cell.ensureSorted()
   */
  // sort by estTotalLogP: for pruning purpose
  public int compareTo(HGNode anotherItem) {
    System.out.println("HGNode, compare functiuon should never be called");
    System.exit(1);
    return 0;
    /*
     * if (this.estTotalLogP > anotherItem.estTotalLogP) { return -1; } else if (this.estTotalLogP
     * == anotherItem.estTotalLogP) { return 0; } else { return 1; }
     */

  }

  /**
   * This sorts nodes by span, useful when dumping the hypergraph.
   */
  public static Comparator<HGNode> spanComparator = new Comparator<HGNode>() {
    public int compare(HGNode item1, HGNode item2) {
      int span1 = item1.j - item1.i;
      int span2 = item2.j - item2.i;
      if (span1 < span2)
        return -1;
      else if (span1 > span2)
        return 1;
      else if (item1.i < item2.i)
        return -1;
      else if (item1.i > item2.i)
        return 1;
      return 0;
    }
  };

  public static Comparator<HGNode> inverseLogPComparator = new Comparator<HGNode>() {
    public int compare(HGNode item1, HGNode item2) {
      double logp1 = item1.estTotalLogP;
      double logp2 = item2.estTotalLogP;
      if (logp1 > logp2) {
        return -1;
      } else if (logp1 == logp2) {
        return 0;
      } else {
        return 1;
      }
    }
  };

  /**
   * natural order
   * */
  public static Comparator<HGNode> logPComparator = new Comparator<HGNode>() {
    public int compare(HGNode item1, HGNode item2) {
      double logp1 = item1.estTotalLogP;
      double logp2 = item2.estTotalLogP;
      if (logp1 > logp2) {
        return 1;
      } else if (logp1 == logp2) {
        return 0;
      } else {
        return -1;
      }
    }
  };

  public boolean isDead() {
    return this.isDead;
  }

  public double getPruneLogP() {
    return this.estTotalLogP;
  }

  public void setDead() {
    this.isDead = true;
  }

  public void setPruneLogP(double estTotalLogP) {
    this.estTotalLogP = estTotalLogP;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("%s (%d,%d) score=%.5f", Vocabulary.word(lhs), i, j,
        bestHyperedge.bestDerivationLogP));
    if (dpStates != null)
      for (DPState state : dpStates.values())
        sb.append(" <" + state + ">");

    // if (this.hyperedges != null) {
    // sb.append(" hyperedges: " + hyperedges.size());
    // for (HyperEdge edge: hyperedges) {
    // sb.append("\n\t" + edge.getRule() + " ||| pathcost=" + edge.getSourcePath() + " ref="+
    // Integer.toHexString(edge.hashCode()));
    // }
    // }

    // sb.append("\n\ttransition score = " + bestHyperedge.getTransitionLogP(true));
    return sb.toString();
  }

  public List<HyperEdge> getHyperEdges() {
    return this.hyperedges;
  }
}
