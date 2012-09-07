package joshua.decoder.ff;

import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.chart_parser.SourcePath;
import joshua.corpus.Vocabulary;


/**
 * This feature handles the list of dense features that may be associated with the rules in a
 * grammar file.  The feature names of these dense rules are a function of the phrase model owner.
 * When the feature is loaded, it queries the weights for the set of features that are active for
 * this grammar, storing them in an array.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li <zhifei.work@gmail.com>
 */

public class PhraseModelFF extends PrecomputableFF {

  private static final Logger logger = Logger.getLogger(PhraseModelFF.class.getName());

	/* An array for storing the dense set of features */
	private float[] featureWeights;

	/* The owner of the grammar. */
	private String owner;
	private int ownerID;

  public PhraseModelFF(FeatureVector weights, GrammarFactory grammar, String owner) {
    super(weights, "PhraseModel_" + owner, "");

		// Store the owner.
		this.owner = owner;
		this.ownerID = Vocabulary.id(owner);
		
		/* Now query the weights to see how many features there are active for this template.  We go
		 * through, finding all weights of the form "PhraseModel_OWNER_NUMBER".  The highest NUMBER is
		 * the length of the dense array.  We record the weights for quick and easy application when we
		 * are later asked to score a rule. 
		 */
		String prefix = name + "_";
		int maxIndex = 0;
		for (String key: weights.keySet()) {
			if (key.startsWith(prefix)) {
				int index = Integer.parseInt(key.substring(key.lastIndexOf("_") + 1));
				if (index > maxIndex) 
					maxIndex = index;
			}
		}
		featureWeights = new float[maxIndex+1];
		for (int i = 0; i <= maxIndex; i++) {
			String key = String.format("%s_%d", name, i);
				featureWeights[i] = (weights.containsKey(key))
					? weights.get(key)
					: 0.0f;
		}
	}


	/**
	 * Compute the features triggered by the supplied rule.
	 */
	public FeatureVector computeFeatures(final Rule rule) {
		FeatureVector featureDelta = new FeatureVector();

		float[] featureScores = rule.getDenseFeatures();
		for (int i = 0; i < featureScores.length; i++)
			featureDelta.put(String.format("PhraseModel_%s_%d", owner, i), featureScores[i]);

		return featureDelta;
	}


	/**
	 * Computes the cost of applying the feature.
	 */
  public float computeCost(final Rule rule) {
		float cost = 0.0f;

    if (this.ownerID == rule.getOwner() && rule.getDenseFeatures() != null) {
			float[] featureScores = rule.getDenseFeatures();
			for (int i = 0; i < featureWeights.length; i++)
				cost += featureWeights[i] * featureScores[i];
    }

		return cost;
  }
}
