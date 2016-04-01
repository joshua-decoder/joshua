package joshua.decoder.hypergraph;

import static joshua.decoder.chart_parser.ComputeNodeResult.computeTransitionFeatures;

import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.segment_file.Sentence;

public class ViterbiFeatureVectorWalkerFunction implements WalkerFunction {
  
  private final FeatureVector features;
  private final List<FeatureFunction> featureFunctions;
  private final Sentence sourceSentence;
  
  public ViterbiFeatureVectorWalkerFunction(
      final List<FeatureFunction> featureFunctions,
      final Sentence sourceSentence) {
    this.features = new FeatureVector();
    this.featureFunctions = featureFunctions;
    this.sourceSentence = sourceSentence;
  }

  /**
   * Recompute feature values for each Viterbi edge and add to features.
   */
  @Override
  public void apply(HGNode node) {
    final FeatureVector edgeFeatures = computeTransitionFeatures(
        featureFunctions, node.bestHyperedge, node.i, node.j, sourceSentence);
    features.add(edgeFeatures);
  }
  
  public FeatureVector getFeatures() {
    return features;
  }
  
  public Map<String,Float> getFeaturesMap() {
    return features.getMap();
  }

}
