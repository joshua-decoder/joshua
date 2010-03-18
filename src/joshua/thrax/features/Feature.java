package joshua.thrax.features;

import joshua.decoder.ff.tm.Rule;

/**
 * The common interface that all feature functions for rules must implement.
 */
public interface Feature {

	/**
	 * Stores any needed information from a newly-extracted rule in state
	 * for this feature function. For example, it may increase a count
	 * to see that a rule has been extracted another time.
	 *
	 * @param r the Rule extracted
	 */
	public void noteExtraction(Rule r);

	/**
	 * Returns the score for the current rule under this feature function.
	 *
	 * @param r the Rule
	 * @return score for that rule
	 */
	public float score(Rule r);

}
