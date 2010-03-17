package joshua.thrax.extractor;

import joshua.thrax.corpus.AlignedBitext;

public class HieroExtractor extends HierarchicalExtractor {

	private int X;
	public HieroExtractor(AlignedBitext bt, int len)
	{
		super(bt, len);
		X = bt.getSourceCorpus().getVocabulary().X;
	}

	int getLhsNonterminal(HierarchicalSpan h)
	{
		return X;
	}

	int [] getRhsNonterminals(HierarchicalSpan h)
	{
		int [] ret = new int[h.arity];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = X;
		}
		return ret;
	}
}
