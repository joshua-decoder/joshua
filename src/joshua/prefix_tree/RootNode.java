/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.prefix_tree;

import java.util.Collections;
import java.util.List;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.suffix_array.HierarchicalPhrases;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;

/**
 * Root node of a prefix tree.
 *
 * @author Lane Schwartz
 */
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
			setBounds(0, suffixArray.size()-1);
		}
	}
	
	/**
	 * Gets an empty list of rules.
	 * 
	 * @return an empty list of rules
	 */
	protected List<Rule> getResults() {
		return Collections.emptyList();
	}
	
	/**
	 * Gets an empty list of matched hierarchical phrases.
	 * <p>
	 * The list of hierarchical phrases 
     * for the X node that comes off of ROOT 
     * is defined to be an empty list.
     * <p>
     * One could alternatively consider 
     * every phrase in the corpus to match here,
     * but we don't.
     * 
     * @return an empty list of matched hierarchical phrases
	 */
	protected MatchedHierarchicalPhrases getMatchedPhrases()  {
		return matchedPhrases;
	}
	
	public String toString() {
		return toString(tree.vocab, PrefixTree.ROOT_NODE_ID);
	}
	
	public Node addChild(int child) {
		if (child==SymbolTable.X) {
			if (children.containsKey(child)) {
				throw new ChildNodeAlreadyExistsException(this, child);
			} else {
				XNode node = new XNode(this);
				children.put(child, node);
				return node;
			}
		} else {
			return super.addChild(child);
		}

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
