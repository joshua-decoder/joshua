package joshua.prefix_tree;

import java.util.Collections;
import java.util.List;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.suffix_array.HierarchicalPhrases;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;

public class RootNode extends Node implements Grammar {

	private final PrefixTree tree;
	private final MatchedHierarchicalPhrases matchedPhrases;
	
	RootNode(PrefixTree tree, int incomingArcValue) {
		super(tree.parallelCorpus, 1);
		this.tree = tree;
		SymbolTable vocab = tree.vocab;
		this.matchedPhrases = HierarchicalPhrases.emptyList(vocab);
		Suffixes suffixArray = tree.suffixArray;
		if (suffixArray != null) {
//			vocab = suffixArray.getVocabulary();
			//int[] bounds = {0, suffixArray.size()-1};
			setBounds(0, suffixArray.size()-1);
		}
	}
	
	protected List<Rule> getResults() {
		return Collections.emptyList();
	}
	
	protected MatchedHierarchicalPhrases getMatchedPhrases()  {
		return matchedPhrases;
	}
	
	public String toString() {
		return toString(tree.vocab, PrefixTree.ROOT_NODE_ID);
	}
	
	String toTreeString(String tabs, SymbolTable vocab) {
		return super.toTreeString(tabs, vocab, PrefixTree.ROOT_NODE_ID);
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
