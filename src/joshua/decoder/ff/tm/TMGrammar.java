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
package joshua.decoder.ff.tm;

import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;
import joshua.util.sentence.Phrase;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * public interfaces
 * TMGrammar: init and load the grammar
 * TrieGrammar: match symbol for next layer
 * RuleBin: get sorted rules
 * Rule: rule information
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class TMGrammar implements GrammarFactory, Grammar {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	public    static int OOV_RULE_ID          = 0;
	protected static ArrayList<FeatureFunction> p_l_models = null;
	protected static int defaultOwner         = 0; //will change autotmatically
	protected  static String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";//e.g., [X,1]
	protected static String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
	
	protected int spanLimit = 10;
	
	private static final Logger logger = Logger.getLogger(TMGrammar.class.getName());
	
	public TMGrammar(
		String grammar_file,
		ArrayList<FeatureFunction> l_models,
		final String default_owner,
		final int    span_limit,
		final String nonterminal_regexp,
		final String nonterminal_replace_regexp
	) {	
		// BUG: These are all static, should not be set by constructor!
		p_l_models               = l_models;
		defaultOwner             = Symbol.add_terminal_symbol(default_owner);
		nonterminalRegexp        = nonterminal_regexp;
		nonterminalReplaceRegexp = nonterminal_replace_regexp;
		
		this.spanLimit = span_limit;
	}
	
	
	public abstract TrieGrammar getTrieRoot();
	
	
	public Grammar getGrammarForSentence(Phrase sentence) {
		return this;
	}
	
	
	/** if the span covered by the chart bin is greater than the limit, then return false */
	public boolean hasRuleForSpan(
		final int startIndex,
		final int endIndex,
		final int pathLength
	) {
		if (this.spanLimit == -1) { // mono-glue grammar
			return (startIndex == 0);
		} else {
			return (endIndex - startIndex <= this.spanLimit);
		}
	}
	
	
	//TODO: we assume all the Chinese training text is lowercased, and all the non-terminal symbols are in [A-Z]+
	public  static final boolean is_non_terminal(final String symbol) {
		return symbol.matches(TMGrammar.nonterminalRegexp);
	}
	
	
	//contain all rules with the same french side (and thus same arity)
	public abstract class RuleBin implements RuleCollection {
		protected int arity = 0;//number of non-terminals
		protected int[] french;
	}
}
