package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * Implements precomputable feature functions.
 *
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */

public abstract class PrecomputableFF extends StatelessFF {

  public PrecomputableFF(FeatureVector weights, String name) {
    super(weights, name);
  }

  public PrecomputableFF(FeatureVector weights, String name, String args) {
    super(weights, name, args);
  }

  public FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID) {
		return computeFeatures(rule);
	}

  /**
   * Computes the features and their values induced by applying this rule.  This is used for the
   * k-best extraction code, and should also be called from ComputeCost().  Makes use of the
   * FeatureVector class, but note this contains feature values and not weights.
   */
  public abstract FeatureVector computeFeatures(Rule rule);


  public float computeCost(Rule rule, SourcePath sourcePath, int sentID) {
		return computeCost(rule, null, -1);
	}

  /**
   * Return the cost of applying a rule for a particular sentence.  The cost is the inner product of
   * (1) the feature vector of features that fire on this rule and (2) the associated weights from
   * the weight vector.
   *
	 * This function should be overridden to be made more efficient than the hash * lookup defined
	 * here; this default implementation assumes the feature value is 1 and multiplies * it times the
	 * weight obtained inefficiently from the hash.
   */
  public float computeCost(Rule rule) {
    if (name != null) {
      FeatureVector features = computeFeatures(rule);
      if (weights.containsKey(name) && features.containsKey(name))
        return weights.get(name) * features.get(name);
    }
    return 0.0f;
  }
}
 
