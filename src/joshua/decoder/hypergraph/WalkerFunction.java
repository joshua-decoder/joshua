package joshua.decoder.hypergraph;

/**
 * Classes implementing this interface define a single function that is applied to each node. This
 * interface is used for various walkers (ViterbiExtractor, ForestWalker).
 */
public interface WalkerFunction {

  void apply(HGNode node);

}
