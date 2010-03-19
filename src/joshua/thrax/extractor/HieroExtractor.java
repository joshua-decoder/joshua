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
	private static String X = "[X]";
	private static String NT_TEMPLATE = "[X,%d]";

	// TODO:
	// factor this out of here and hierarchical span. should only be
	// given once.
	public static final int ARITY_LIMIT = 2;

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
		bt.getSourceCorpus().getVocabulary().addNonterminal(X);
		bt.getTargetCorpus().getVocabulary().addNonterminal(X);
		for (int i = 0; i < ARITY_LIMIT; i++) {
			bt.getSourceCorpus().getVocabulary().addNonterminal(String.format(NT_TEMPLATE, i + 1));
			bt.getTargetCorpus().getVocabulary().addNonterminal(String.format(NT_TEMPLATE, i + 1));
		}
		
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
		ret.add(new BilingualRule(bitext.getSourceCorpus().getVocabulary().getID(X), src, tgt, scores, h.arity));
		return ret;
	}

	private void populate(int arity, Span root, int [] nts, int [] result,
	                      Corpus cor)
	{
		int resultIndex = 0;
		int start = root.start;
		int currSym = 0;
		for (int i : root) {
			// for each entry in the NT template
			int nt = nts[i-start];
			if (nt == 0) {
				// terminal symbol case
				currSym = 0;
				result[resultIndex] = cor.getWordID(i);
				resultIndex++;
			}
			else {
				// skip over the rest of this span,
				// because it's been replaced with one NT
				if (nt == currSym) {
					continue;
				}
				currSym = nt;
				int ntSym = cor.getVocabulary().getID(String.format(NT_TEMPLATE, -nt));
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
		return 0; //X; TODO: remove these methods
	}

	/**
	 * Returns enough copies of X to populate the right hand side of an
	 * extracted rule.
	 */
	int [] getRhsNonterminals(HierarchicalSpan h)
	{
		int [] ret = new int[h.arity];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = 0; //X; TODO: remove these methods
		}
		return ret;
	}
}
