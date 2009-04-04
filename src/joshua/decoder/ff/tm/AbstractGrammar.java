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

/**
 * Grammar is a class for wrapping a trie of TrieGrammar in order
 * to store holistic metadata.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * 
 * @version $LastChangedDate$
 */

import java.util.ArrayList;

import joshua.decoder.ff.FeatureFunction;


public abstract class AbstractGrammar implements Grammar {
 
	/**
	 * Cube-pruning requires that the grammar be sorted based on the latest feature functions.
	 */
	public void sortGrammar(ArrayList<FeatureFunction> models) {
		sort(getTrieRoot(), models);
	}
	
	private void sort(Trie node, ArrayList<FeatureFunction> models) {
		if (node != null) {
			node.getRules().sortRules(models);
			
			for (Trie child : node.getExtensions()) {
				sort(child, models);
			}
		}
	}

}
