package joshua.thrax.extractor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.Span;
import joshua.corpus.Corpus;
import joshua.corpus.vocab.SymbolTable;
import joshua.thrax.corpus.AlignedBitext;
import joshua.thrax.corpus.AlignedParallelPhrase;

public class HierarchicalExtractor implements Extractor {

	private int ruleLengthLimit;
	private AlignedBitext bitext;

	public HierarchicalExtractor(AlignedBitext bt, int len)
	{
		this.bitext = bt;
		this.ruleLengthLimit = len;
	}

	public List<Rule> getAllRules()
	{
		ArrayList<Rule> result = new ArrayList<Rule>();

		for (AlignedParallelPhrase p : bitext) {
			for (Rule r : allRules(p)) {
				result.add(r);
			}
		}
		return result;
	}

	private Set<Rule> allRules(AlignedParallelPhrase p)
	{
		Alignments alignments = p.getAlignment();
		Set<Rule> result = new HashSet<Rule>();
		List<HierarchicalSpan> rulePatterns = new ArrayList<HierarchicalSpan>();

		for (Span s : p.getSpan().getSubSpans(ruleLengthLimit)) {
			Span t = alignments.getConsistentTargetSpan(s);
			if (t == null) {
				continue;
			}
			rulePatterns.add(new HierarchicalSpan(s, t));
			for (int i = 0; i < rulePatterns.size(); i++) {
				HierarchicalSpan hs = rulePatterns.get(i);
				if (hs.consistentWith(s, t)) {
					HierarchicalSpan x = hs.add(s, t);
					rulePatterns.add(x);
				}
			}
		}

		for (HierarchicalSpan hs : rulePatterns) {
			result.add(createRule(hs));
		}
		return result;
	}

	private Rule createRule(HierarchicalSpan h)
	{

		int arity = h.arity;

		int sourceRhsSize = h.sourceRhsSize;
		int targetRhsSize = h.targetRhsSize;
		SymbolTable sourceVocab = bitext.getSourceCorpus().getVocabulary();
		int lhs = sourceVocab.X;
		int [] sourceRhs = new int[sourceRhsSize];
		int [] targetRhs = new int[targetRhsSize];
		Corpus sourceCorpus = bitext.getSourceCorpus();
		Corpus targetCorpus = bitext.getTargetCorpus();

		// TODO: deal with scores.
		float [] scores = new float[1];
	

		return new BilingualRule(lhs, sourceRhs, targetRhs, scores, arity);
	}
}
