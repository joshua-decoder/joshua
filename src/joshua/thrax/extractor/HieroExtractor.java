package joshua.thrax.extractor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import joshua.decoder.ff.tm.Rule;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.Span;
import joshua.thrax.corpus.AlignedBitext;
import joshua.thrax.corpus.ParallelSpan;

public class HieroExtractor implements Extractor {

	private int ruleLengthLimit;
	private AlignedBitext bitext;

	public HieroExtractor(AlignedBitext bt, int len)
	{
		this.bitext = bt;
		this.ruleLengthLimit = len;
	}

	public Map<Rule,Integer> getAllRuleCounts()
	{
		HashMap<Rule,Integer> result = new HashMap<Rule,Integer>();

		for (ParallelSpan p : bitext) {
			for (Rule r : allRules(p)) {
				if (result.containsKey(r)) {
					result.put(r, result.get(r) + 1);
				}
				else {
					result.put(r, 1);
				}
			}
		}
		return result;
	}

	private Set<Rule> allRules(ParallelSpan p)
	{
		Alignments alignments = bitext.getAlignments();
		return new HashSet<Rule>();
	}
}
