package joshua.oracle;

import joshua.decoder.hypergraph.HyperGraph;

/**
 * Convenience wrapper class for oracle extraction code.
 * 
 * @author Lane Schwartz
 */
public class OracleExtractor {

  private final OracleExtractionHG extractor;

  /**
   * Constructs an object capable of extracting an oracle hypergraph.
   */
  public OracleExtractor() {

    int baselineLanguageModelFeatureID = 0;
    this.extractor = new OracleExtractionHG(baselineLanguageModelFeatureID);

  }

  /**
   * Extract a hypergraph that represents the translation from the original shared forest hypergraph
   * that is closest to the reference translation.
   * 
   * @param forest Original hypergraph representing a shared forest.
   * @param lmOrder N-gram order of the language model.
   * @param reference Reference sentence.
   * @return Hypergraph closest to the reference.
   */
  public HyperGraph getOracle(HyperGraph forest, int lmOrder, String reference) {
    if (reference != null)
      return extractor.oracle_extract_hg(forest, forest.sentLen, lmOrder, reference);

    return null;
  }

}
