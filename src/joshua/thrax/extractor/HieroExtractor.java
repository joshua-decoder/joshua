package joshua.thrax.extractor;

import joshua.thrax.corpus.AlignedBitext;

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
