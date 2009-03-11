/**
 * 
 */
package joshua.decoder.ff.tm.HieroGrammar;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;


public class MemoryBasedTrie implements Trie {
		MemoryBasedRuleBin rule_bin     = null;
		HashMap<Integer,MemoryBasedTrie> tbl_children = null;
		
		
		//looking for the next layer trinode corresponding to this symbol
		public MemoryBasedTrie matchOne(int sym_id) {
			if (null == tbl_children) {
				return null;
			} else {
				return tbl_children.get(sym_id);
			}
		}
		
		
		public boolean hasExtensions() {
			return (null != this.tbl_children);
		}
		
		public HashMap<Integer,MemoryBasedTrie>  getExtensions() {
			return this.tbl_children;
		}
		
		public void setExtensions(HashMap<Integer,MemoryBasedTrie> tbl_children_) {
			this.tbl_children = tbl_children_;
		}
		
		public boolean hasRules() {
			return (null != this.rule_bin);
		}
		
		
		public void setRuleBin(MemoryBasedRuleBin rb) {
			rule_bin = rb;
		}
		
		public RuleCollection getRules() {
			return this.rule_bin;
		}
		
		
		//recursive call, to make sure all rules are sorted
		public void ensure_sorted(ArrayList<FeatureFunction> l_models) {
			if (null != this.rule_bin) {
				this.rule_bin.getSortedRules(l_models);
			}
			if (null != this.tbl_children) {
				Object[] tem = this.tbl_children.values().toArray();
				for (int i = 0; i < tem.length; i++) {
					((MemoryBasedTrie)tem[i]).ensure_sorted(l_models);
				}
			}
		}
		
/* TODO Possibly remove - this method is never called.		
		private void print_info(int level) {
			Support.write_log_line("###########TrieGrammar###########",level);
			if (null != rule_bin) {
				Support.write_log_line("##### RuleBin(in TrieGrammar) is",level);
				rule_bin.print_info(level);
			}
			if (null != tbl_children) {
				Object[] tem = tbl_children.values().toArray();
				for (int i = 0; i < tem.length; i++) {
					Support.write_log_line("##### ChildTrieGrammar(in TrieGrammar) is",level);
					((TrieNode_Memory)tem[i]).print_info(level);
				}
			}
		}
*/
	}