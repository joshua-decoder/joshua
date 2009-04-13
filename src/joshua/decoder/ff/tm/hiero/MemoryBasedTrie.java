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
package joshua.decoder.ff.tm.hiero;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/
public class MemoryBasedTrie implements Trie {
	MemoryBasedRuleBin rule_bin = null;
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
	
	public HashMap<Integer,MemoryBasedTrie> getExtensionsTable() {
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
//			this.rule_bin.getSortedRules(l_models);
			this.rule_bin.sortRules(l_models);
		}
		if (null != this.tbl_children) {
			Object[] tem = this.tbl_children.values().toArray();
			for (int i = 0; i < tem.length; i++) {
				((MemoryBasedTrie)tem[i]).ensure_sorted(l_models);
			}
		}
	}
	
	
	public Collection<MemoryBasedTrie> getExtensions() {
		return this.tbl_children.values();
//		Object[] tem = this.tbl_children.values().toArray();
//		Trie[] res = new Trie[tem.length];
//		for(int i = 0; i < tem.length; i++) {
//			res[i] = (Trie) tem[i];
//		}
//		return res;
	}
	
	
	public Trie[] getExtensionsWrong() {
		Trie[] tem = (Trie[]) this.tbl_children.values().toArray();
		return tem;
	}
}