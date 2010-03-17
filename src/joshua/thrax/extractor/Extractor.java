package joshua.thrax.extractor;

import java.util.List;
import joshua.decoder.ff.tm.Rule;

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
	 * @return a list of the extracted rules
	 */
	public List<Rule> getAllRules();

}
