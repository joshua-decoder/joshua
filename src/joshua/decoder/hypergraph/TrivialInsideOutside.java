package joshua.decoder.hypergraph;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public class TrivialInsideOutside extends DefaultInsideOutside {
  // used by inside-outside estimation
  protected double getHyperedgeLogProb(HyperEdge dt, HGNode parent_it) {
    return dt.getTransitionLogP(false);// TODO this is very bad in terms of computation
  }
}
