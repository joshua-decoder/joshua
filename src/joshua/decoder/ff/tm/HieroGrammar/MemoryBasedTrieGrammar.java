/**
 * 
 */
package joshua.decoder.ff.tm.HieroGrammar;

import java.util.HashMap;

import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.TrieGrammar;


public class MemoryBasedTrieGrammar implements TrieGrammar {
		MemoryBasedRuleBin rule_bin     = null;
		HashMap<Integer,MemoryBasedTrieGrammar> tbl_children = null;
		
		
		//looking for the next layer trinode corresponding to this symbol
		public MemoryBasedTrieGrammar matchOne(int sym_id) {
			if (null == tbl_children) {
				return null;
			} else {
				return tbl_children.get(sym_id);
			}
		}
		
		
		public boolean hasExtensions() {
			return (null != this.tbl_children);
		}
		
		
		public boolean hasRules() {
			return (null != this.rule_bin);
		}
		
		
		public RuleCollection getRules() {
			return this.rule_bin;
		}
		
		
		//recursive call, to make sure all rules are sorted
		void ensure_sorted() {
			if (null != this.rule_bin) {
				this.rule_bin.getSortedRules();
			}
			if (null != this.tbl_children) {
				Object[] tem = this.tbl_children.values().toArray();
				for (int i = 0; i < tem.length; i++) {
					((MemoryBasedTrieGrammar)tem[i]).ensure_sorted();
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