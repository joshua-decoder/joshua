package joshua.thrax.extractor;

import java.util.Map;
import joshua.decoder.ff.tm.Rule;

public interface Extractor {

	public Map<Rule,Integer> getAllRuleCounts();

}
