package joshua.decoder.ff;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;

/**
 * This feature returns the scored path through the source lattice, which is recorded in a
 * SourcePath object.
 * 
 * @author Chris Dyer <redpony@umd.edu>
 * @author Matt Post <post@cs.jhu.edu>
 */
public final class SourcePathFF extends StatelessFF {

  /*
   * This is a single-value feature template, so we cache the weight here.
   */
  private float weight;

  public SourcePathFF(FeatureVector weights) {
    super(weights, "SourcePath", ""); // this sets name

    // Find the weight for this feature in the weights hash and cache it.
    if (weights.containsKey(name)) {
      weight = weights.get(name);
    } else {
      System.err.println("* WARNING: no weight for feature '" + name + "'");
      weight = 0.0f;
    }
  }
  
  @Override
  public FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID) {
    return new FeatureVector(name, sourcePath.getPathCost());
  }

  @Override
  public float computeCost(Rule rule, SourcePath sourcePath, int sentID) {
    return weight * sourcePath.getPathCost();
  }
}
