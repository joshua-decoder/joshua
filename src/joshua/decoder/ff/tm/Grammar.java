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
import joshua.decoder.ff.tm.Trie;


/**
 * Grammar is a class for wrapping a trie of TrieGrammar in order
 * to store holistic metadata.
 * 
 * @author wren ng thornton
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface Grammar {
	
	/**
	 * Returns the root of the trie (as a small constant-time function).
	 */
	public Trie getTrieRoot();
	
	
	/**
	 * Returns whether this grammar has any valid rules for
	 * covering a particular span of a sentence. Heiro's "glue"
	 * grammar will only say True if the span is longer than
	 * our span limit, and is anchored at startIndex==0. Heiro's
	 * "regular" grammar will only say True if the span is less
	 * than the span limit. Other grammars, e.g. for rule-based
	 * systems, may have different behaviors.
	 */
	public boolean hasRuleForSpan(int startIndex, int endIndex,	int pathLength);
	
	
	/**
	 * Cube-pruning requires that the grammar be sorted based on the latest feature functions.
	 */
	public void sortGrammar(ArrayList<FeatureFunction> models);
	
	
	/** construct an oov rule for the word source 
	 * only called when creating oov rule in Chart or DiskHypergraph, all
	 * the transition cost for phrase model, arity penalty,
	 * word penalty are all zero, except the LM cost or the first feature if no LM feature is used
	 *TODO: will try to get rid of owner, have_lm_model, and num_feats
	 */
	public Rule constructOOVRule(int num_feats, int lhs, int sourceWord, int owner, boolean have_lm_model);
	
	
	/** return the OOV rule ID
	 * */
	public int getOOVRuleID();
}
