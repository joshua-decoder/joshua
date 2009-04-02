package joshua.decoder.ff.tm;

import java.util.ArrayList;

import joshua.decoder.ff.FeatureFunction;


public abstract class AbstractGrammar implements Grammar {
 
	public static int OOV_RULE_ID = 0;
	
	public Rule constructOOVRule(int qtyFeatures, int lhs, int sourceWord, int owner, boolean hasLM) {
		int[] p_french      = new int[1];
		p_french[0]         = sourceWord;
		int[] english       = new int[1];
		english[0]          = sourceWord;
		float[] feat_scores = new float[qtyFeatures];
		
		// TODO: This is a hack to make the decoding without a LM works
		if (! hasLM) { // no LM is used for decoding, so we should set the stateless cost
			//this.feat_scores[0]=100.0/(this.featureFunctions.get(0)).getWeight();//TODO
			feat_scores[0] = 100; // TODO
		}
		
		return new BilingualRule(lhs, p_french, english, feat_scores, 0, owner, 0, getOOVRuleID());
	}

	public int getOOVRuleID() {
		return OOV_RULE_ID;
	}


	/**
	 * Cube-pruning requires that the grammar be sorted based on the latest feature functions.
	 */
	public void sortGrammar(ArrayList<FeatureFunction> models) {
		sort(getTrieRoot(), models);
	}
	
	private void sort(Trie node, ArrayList<FeatureFunction> models) {
		if (node != null) {
			node.getRules().sortRules(models);
			
			for (Trie child : node.getExtensions()) {
				sort(child, models);
			}
		}
	}

}
