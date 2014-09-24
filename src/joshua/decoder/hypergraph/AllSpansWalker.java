package joshua.decoder.hypergraph;

import java.util.HashSet;
import java.util.Set;

import joshua.corpus.Span;

/***
 * Uses {@link ForestWalker} to visit one {@link HGNode} per span of the chart. No guarantees are
 * provided as to which HGNode will be visited in each span.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * 
 */

public class AllSpansWalker {
  private Set<Span> visitedSpans;

  public AllSpansWalker() {
    visitedSpans = new HashSet<Span>();
  }

  /**
   * This function wraps a {@link ForestWalker}, preventing calls to its walker function for all but
   * the first node reached for each span.
   * 
   * @param node
   * @param walker
   */
  public void walk(HGNode node, final WalkerFunction walker) {
    new ForestWalker().walk(node, new joshua.decoder.hypergraph.WalkerFunction() {
      @Override
      public void apply(HGNode node) {
        if (node != null) {
          Span span = new Span(node.i, node.j);
          if (!visitedSpans.contains(span)) {
            walker.apply(node);
            visitedSpans.add(span);
          }
        }
      }
    });
  }
}
