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

import java.util.ArrayList;

import joshua.decoder.ff.FeatureFunction;

/**
 * Partial implementation of the <code>Grammar</code> interface
 * that provides logic for sorting a grammar.
 * <p>
 * <em>Note</em>: New classes implementing the <code>Grammar</code>
 * interface should probably inherit from this class, unless a
 * specific sorting technique different from that implemented by
 * this class is required.
 *
 * @author Lane Schwartz
 */
public abstract class AbstractGrammar implements Grammar {
 
	/** 
	 * Indicates whether the rules in this grammar have been
	 * sorted based on the latest feature function values.
	 */
	protected boolean sorted;
	
	/**
	 * Constructs an empty, unsorted grammar.
	 *
	 * @see Grammar#isSorted()
	 */
	public AbstractGrammar() {
		this.sorted = false;
	}
	
	/**
	 * Cube-pruning requires that the grammar be sorted based
	 * on the latest feature functions. To avoid synchronization,
	 * this method should be called before multiple threads are
	 * initialized for parallel decoding
	 */
	public void sortGrammar(ArrayList<FeatureFunction> models) {
		Trie root = getTrieRoot();
		if(root!=null){
			sort(getTrieRoot(), models);
			setSorted(true);
		}
	}

	/* See Javadoc comments for Grammar interface. */
	public boolean isSorted() {
		return sorted;
	}
	
	/**
	 * Sets the flag indicating whether this grammar is sorted.
	 * <p>
	 * This method is called by {@link #sortGrammar(ArrayList)}
	 * to indicate that the grammar has been sorted.
	 * 
	 * Its scope is protected so that child classes that override
	 * <code>sortGrammar</code> will also be able to call this
	 * method to indicate that the grammar has been sorted.
	 * 
	 * @param sorted
	 */
	protected void setSorted(boolean sorted) {
		this.sorted = sorted;
	}
	
	/**
	 * Recursively sorts the grammar using the provided feature
	 * functions.
	 * <p>
	 * This method first sorts the rules stored at the provided
	 * node, then recursively calls itself on the child nodes
	 * of the provided node.
	 * 
	 * @param node   Grammar node in the <code>Trie</code> whose
	 *               rules should be sorted.
	 * @param models Feature function models to use during
	 *               sorting.
	 */
	private void sort(Trie node, ArrayList<FeatureFunction> models) {
		if (node != null) {
			if(node.hasRules())
				node.getRules().sortRules(models);
			if(node.hasExtensions()){
				for (Trie child : node.getExtensions()) {
					sort(child, models);
				}
			}
		}
	}
	
}
