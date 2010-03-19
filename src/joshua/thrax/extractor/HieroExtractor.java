package joshua.thrax.extractor;

import java.util.HashSet;
import java.util.Set;
import joshua.thrax.corpus.AlignedBitext;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.corpus.Span;
import joshua.corpus.Corpus;

/**
 * An extractor for Hiero-style rules. The only possible nonterminal is X.
 */
public class HieroExtractor extends HierarchicalExtractor {

	/**
	 * The nonterminal!
	 */
	private int X;

	/**
	 * Constructor. Finds the appropriate X value from the source
	 * corpus of the bitext.
	 *
	 * @param bt an aligned parallel corpus
	 * @param len the rule length limit for extracted rules
	 */
	public HieroExtractor(AlignedBitext bt, int len)
	{
		super(bt, len);
		X = bt.getSourceCorpus().getVocabulary().X;
	}

	Set<Rule> createRules(HierarchicalSpan h)
	{
		HashSet<Rule> ret = new HashSet<Rule>();
		// System.err.println(h.ntTemplate());

		int [] src = new int[h.sourceRhsSize];
		int [] tgt = new int[h.targetRhsSize];

		// terrible hack until the new vocabulary arrives
		// assuming -2 -> [X,1] and -3 -> [X,2]
		populate(h.arity, h.sourceRoot, h.sourceRhs, src, bitext.getSourceCorpus());
		populate(h.arity, h.targetRoot, h.targetRhs, tgt, bitext.getTargetCorpus());

		float [] scores = new float[features.size()];
		ret.add(new BilingualRule(X, src, tgt, scores, h.arity));
		return ret;
	}

	private void populate(int arity, Span root, int [] nts, int [] result,
	                      Corpus cor)
	{
		int resultIndex = 0;
		int start = root.start;
		boolean inNonterminal = false;
		for (int i : root) {
			// for each entry in the NT template
			int nt = nts[i-start];
			if (nt == 0) {
				// terminal symbol case
				inNonterminal = false;
				result[resultIndex] = cor.getWordID(i);
				resultIndex++;
			}
			else {
				// skip over the rest of this span,
				// because it's been replaced with one NT
				if (inNonterminal) {
					continue;
				}
				inNonterminal = true;
				int ntSym = X;
				// if arity < 2, the only NT is X again
				if (arity >= 2) {
					// FIXME:
					// this is a terrible hack.
					// we need the new Vocabulary.
					ntSym = nt - 1;
				}
				result[resultIndex] = ntSym;
				resultIndex++;
			}
		}
		return;
	}

	/**
	 * Returns the left hand side nonterminal X.
	 */
	int getLhsNonterminal(HierarchicalSpan h)
	{
		return X;
	}

	/**
	 * Returns enough copies of X to populate the right hand side of an
	 * extracted rule.
	 */
	int [] getRhsNonterminals(HierarchicalSpan h)
	{
		int [] ret = new int[h.arity];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = X;
		}
		return ret;
	}
}
