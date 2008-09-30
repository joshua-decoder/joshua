/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.decoder.ff.tm;

import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;

import java.util.ArrayList;
import java.util.List;


/**
 * public interfaces
 * TMGrammar: init and load the grammar
 * TrieNode: match symbol for next layer
 * RuleBin: get sorted rules
 * Rule: rule information
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class TMGrammar implements GrammarFactory<Integer> {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	public    static int OOV_RULE_ID          = 0;
	protected static ArrayList<FeatureFunction> p_l_models = null;
	protected static int defaultOwner         = 0; //will change autotmatically
	protected static String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";//e.g., [X,1]
	protected static String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
	
	protected int spanLimit = 10;
	
	
	public TMGrammar(
		ArrayList<FeatureFunction> l_models,
		final String default_owner,
		final int    span_limit,
		final String nonterminal_regexp,
		final String nonterminal_replace_regexp
	) {
		// BUG: These are all static, should not be set by constructor!
		this.p_l_models     = l_models;
		this.defaultOwner   = Symbol.add_terminal_symbol(default_owner);
		this.nonterminalRegexp        = nonterminal_regexp;
		this.nonterminalReplaceRegexp = nonterminal_replace_regexp;
		
		this.spanLimit                = span_limit;
	}
	
	public abstract TrieNode get_root();
	
	public TrieGrammar<Integer,Rule> getGrammarForSentence(List<Integer> sentence) {
		throw new RuntimeException("Not yet implemented");
		//TODO Implement this method as:
		//     return get_root();
	}
	
	public abstract void read_tm_grammar_from_file(final String grammar_file);
	
	public abstract void read_tm_grammar_glue_rules();
	
	
	/** if the span covered by the chart bin is greather than the limit, then return false */
	public boolean filter_span(final int start, final int end, final int len) {
		if (this.spanLimit == -1) {//mono-glue grammar
			return (start == 0);
		} else {
			return (end - start <= this.spanLimit);
		}
	}
	
	public static int get_eng_non_terminal_id(final String symbol) {
		//long start = Support.current_time();
		/*String new_str = symbol.replaceAll("[\\[\\]\\,]+", "");//TODO
		int res = new Integer(new_str.substring(new_str.length()-1, new_str.length())).intValue()-1;//TODO: assume only one integer, start from one*/
		int res = Integer.parseInt(symbol.substring(symbol.length() - 2, symbol.length() - 1)) - 1; //TODO: assume only one integer, start from one

		
/*		//// BUG: why are we constructing an Integer only to get its intValue?
		int res = new Integer(
				symbol.substring(symbol.length() - 2, symbol.length() - 1)
			).intValue() - 1; */
		//Chart.g_time_check_nonterminal += Support.current_time()-start;
		return res;
		//return (new Integer(new_str.substring(new_str.length()-1, new_str.length()))).intValue();//TODO: assume only one integer, start from zero
	}
	
	//	TODO: we assume all the Chinese training text is lowercased, and all the non-terminal symbols are in [A-Z]+
	public static final boolean is_non_terminal(final String symbol) {
		return symbol.matches(TMGrammar.nonterminalRegexp);
	}
	
	//DotNode
	public abstract class TrieNode {
		public abstract TrieNode match_symbol(final int sym_id);//find next layer
		public abstract RuleBin get_rule_bin();
		public abstract boolean is_no_child_trienodes();
	}
	
	//contain all rules with the same french side (and thus same arity)
	public abstract class RuleBin implements RuleCollection {
		protected int arity = 0;//number of non-terminals
		protected int[] french;
		
	}
}
