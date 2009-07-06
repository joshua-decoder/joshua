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
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.util.BotMap;

/**
 *
 *
 * @author Lane Schwartz
 */
class BotNode extends Node {
	
	MatchedHierarchicalPhrases matchedPhrases;
	
	BotNode(ParallelCorpusGrammarFactory parallelCorpus, RootNode root) {
		super(parallelCorpus, PrefixTree.BOT_NODE_ID);
		SymbolTable symbolTable = (parallelCorpus==null) ? null : parallelCorpus.getSuffixArray().getVocabulary();
		this.matchedPhrases = HierarchicalPhrases.emptyList(symbolTable);
		this.children = new BotMap<Integer,Node>(root);
	}
	
	protected List<Rule> getResults() {
		return Collections.emptyList();
	}
	
	protected MatchedHierarchicalPhrases getMatchedPhrases()  {
		return matchedPhrases;
	}
}
