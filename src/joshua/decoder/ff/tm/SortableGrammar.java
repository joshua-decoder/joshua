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
 *
 *
 * @author Lane Schwartz
 */
public interface SortableGrammar {

	/**
	 * Gets the root of the <code>Trie</code> backing this
	 * grammar.
	 * <p>
	 * <em>Note</em>: This method should run as a small
	 * constant-time function.
	 * 
	 * @return the root of the <code>Trie</code> backing this
	 *         grammar
	 */
	Trie getTrieRoot();
	
	
	
	/**
	 * After calling this method, the rules in this grammar are
	 * guaranteed to be sorted based on the latest feature
	 * function values.
	 * <p>
	 * Cube-pruning requires that the grammar be sorted based
	 * on the latest feature functions.
	 * 
	 * @param models List of feature functions
	 */
	void sortGrammar(ArrayList<FeatureFunction> models);
	

	
	/** 
	 * Determines whether the rules in this grammar have been
	 * sorted based on the latest feature function values.
	 * <p>
	 * This method is needed for the cube-pruning algorithm.
	 * 
	 * @return <code>true</code> if the rules in this grammar
	 *         have been sorted based on the latest feature
	 *         function values, <code>false</code> otherwise
	 */
	boolean isSorted();
}
