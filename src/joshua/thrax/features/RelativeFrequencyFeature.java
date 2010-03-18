package joshua.thrax.features;

import java.util.HashMap;
import joshua.decoder.ff.tm.Rule;

public class RelativeFrequencyFeature implements Feature {
	private HashMap<Integer,Integer> lhsCounts;
	private HashMap<Rule,Integer> ruleCounts;

	public RelativeFrequencyFeature() {
		lhsCounts = new HashMap<Integer,Integer>();
		ruleCounts = new HashMap<Rule,Integer>();
	}

	public void noteExtraction(Rule r)
	{
		int lhs = r.getLHS();
		int currLhsCount = lhsCounts.containsKey(lhs)
		                 ? lhsCounts.get(lhs) : 0;
		int currRuleCount = ruleCounts.containsKey(r)
		                  ? ruleCounts.get(r) : 0;

		lhsCounts.put(lhs, currLhsCount + 1);
		ruleCounts.put(r, currRuleCount + 1);
		return;
	}

	public float score(Rule r)
	{
		float logP = (float) Math.log(ruleCounts.get(r) / ((double) lhsCounts.get(r.getLHS())));
		return -logP;
	}
}
