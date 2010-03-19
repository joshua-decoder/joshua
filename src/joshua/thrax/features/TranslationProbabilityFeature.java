package joshua.thrax.features;

import joshua.decoder.ff.tm.Rule;
import java.util.HashMap;
import java.util.Arrays;

public class TranslationProbabilityFeature implements Feature {

	private HashMap<Integer, Integer> sourceCounts;
	private HashMap<Integer, Integer> togetherCounts;

	public TranslationProbabilityFeature()
	{
		sourceCounts = new HashMap<Integer, Integer>();
		togetherCounts = new HashMap<Integer, Integer>();
	}

	private int hash(int [] xs, int [] ys)
	{
		return Arrays.hashCode(xs) ^ Arrays.hashCode(ys);
	}

	public void noteExtraction(Rule r)
	{
		int source = Arrays.hashCode(r.getFrench());
		int together = hash(r.getFrench(), r.getEnglish());
		sourceCounts.put(source, 1 + (sourceCounts.containsKey(source) ? sourceCounts.get(source) : 0));
		togetherCounts.put(together, 1 + (togetherCounts.containsKey(together) ? togetherCounts.get(together) : 0));

		return;
	}

	public float score(Rule r)
	{
		int tc = togetherCounts.get(hash(r.getFrench(), r.getEnglish()));
		int sc = sourceCounts.get(Arrays.hashCode(r.getFrench()));
		System.err.println(tc + "/" + sc);
		double ratio = (double) tc / sc;
		float logP = (float) Math.log(ratio);
		return -1 * logP;
	}

}
