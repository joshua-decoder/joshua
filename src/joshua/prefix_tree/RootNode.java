package joshua.prefix_tree;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;

public class RootNode extends Node implements Grammar {

	private final PrefixTree tree;
	
	RootNode(PrefixTree tree, int incomingArcValue) {
		super(tree.vocab, incomingArcValue);
		this.tree = tree;
	}
	
	public String toString() {
		return toString(tree.vocab);
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Grammar#constructOOVRule */
	public Rule constructOOVRule(int num_feats, int sourceWord, boolean have_lm_model) {
		// TODO Auto-generated method stub
		return null;
	}

	/* See Javadoc for joshua.decoder.ff.tm.Grammar#getOOVRuleID */
	public int getOOVRuleID() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* See Javadoc for joshua.decoder.ff.tm.Grammar#constructManualRule */
	public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int aritity) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Grammar#hasRuleForSpan */
	public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
		if (tree.maxPhraseSpan == -1) { // mono-glue grammar
			return (startIndex == 0);
		} else {
			return (endIndex - startIndex <= tree.maxPhraseSpan);
		}
	}
}
