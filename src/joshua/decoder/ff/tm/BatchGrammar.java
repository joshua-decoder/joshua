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


/*This class provides an abstract way to implement BatchGrammar 
 * (meaning the grammar is for the whole test set, not sentence specific)
 * */

public abstract class BatchGrammar implements GrammarFactory, Grammar {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	public    static int OOV_RULE_ID          = 0;
	protected  ArrayList<FeatureFunction> p_l_models = null;
	protected int defaultOwner  ;
	protected  String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";//e.g., [X,1]
	protected String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
	
	protected int spanLimit = 10;
	
	private static final Logger logger = Logger.getLogger(BatchGrammar.class.getName());
	
	Symbol p_symbol = null;
	
	public BatchGrammar(Symbol psymbol, String grammar_file, ArrayList<FeatureFunction> l_models, final String default_owner,	final int span_limit, final String nonterminal_regexp,	final String nonterminal_replace_regexp) {	
		this.p_symbol = psymbol;
		this.p_l_models               = l_models;
		this.defaultOwner             = p_symbol.addTerminalSymbol(default_owner);
		this.nonterminalRegexp        = nonterminal_regexp;
		this.nonterminalReplaceRegexp = nonterminal_replace_regexp;		
		this.spanLimit = span_limit;
	}
	
	
	public abstract TrieGrammar getTrieRoot();
	
	
	public Grammar getGrammarForSentence(Phrase sentence) {
		return this;
	}
	
	
	/** if the span covered by the chart bin is greater than the limit, then return false */
	public boolean hasRuleForSpan(final int startIndex,	final int endIndex,	final int pathLength) {
		if (this.spanLimit == -1) { // mono-glue grammar
			return (startIndex == 0);
		} else {
			return (endIndex - startIndex <= this.spanLimit);
		}
	}
	
	public static final String replace_french_non_terminal(String nonterminalReplaceRegexp_, String symbol) {
		return symbol.replaceAll(nonterminalReplaceRegexp_, "");//remove [, ], and numbers
	}
	
	//TODO: we assume all the Chinese training text is lowercased, and all the non-terminal symbols are in [A-Z]+
	public  static final boolean is_non_terminal(String nonterminalRegexp_, final String symbol) {
		return symbol.matches(nonterminalRegexp_);
	}
	
	
	//contain all rules with the same french side (and thus same arity)
	public abstract class RuleBin implements RuleCollection {
		protected int arity = 0;//number of non-terminals
		protected int[] french;
	}
}
