package joshua.thrax.extractor;

import java.util.Set;
import joshua.decoder.ff.tm.Rule;

import joshua.thrax.features.Feature;

/**
 * This interface represents a rule extractor. It is quite simple at the moment
 * because all we want to do is be able to get a list of rules.
 */
public interface Extractor {

	/**
	 * Extracts all possible rules. In general Thrax usage, a class that
	 * implements Extractor will hold a Corpus internally and extract the
	 * rules from that.
	 *
	 * @return a set of the extracted rules
	 */
	public Set<Rule> getAllRules();

	/**
	 * Registers a feature function to calculate scores for the rules
	 * that are extracted.
	 *
	 * @param f a feature function
	 */
	public void registerFeature(Feature f);

}
