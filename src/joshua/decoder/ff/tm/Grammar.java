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

import joshua.decoder.ff.tm.TrieGrammar;


/**
 * Grammar is a class for wrapping a trie of TrieGrammar in order
 * to store holistic metadata.
 * 
 * @author wren ng thornton
 * @version $LastChangedDate: 2008-09-23 12:52:23 -0400 (Tue, 23 Sep 2008) $
 */
public interface Grammar {
	
	/**
	 * Returns the root of the trie (as a small constant-time function).
	 */
	public TrieGrammar getTrieRoot();
	
	
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
}
