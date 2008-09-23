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
package edu.jhu.joshua.decoder.feature_function.translation_model;

import java.util.Iterator;
import java.util.List;

/**
 * An interface for trie-like data structures. Remember that in the
 * mathematical definition of a trie, each node is isomorphic to an
 * entire trie (just like any subtree of a tree is itself also a
 * tree, and like any tail of a linked list is itself also a linked
 * list).
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-08-03 04:12:57 -0400 (Sun, 03 Aug 2008) $
 */
public interface TrieGrammar<Symbol,Result> {
	
	/**
	 * Traverse one ply further down the trie.
	 */
	TrieGrammar<Symbol,Result> matchOne(Symbol symbol);
	
	
	/**
	 * Traverse some number of plies down the trie. The trivial
	 * implementation of this method just calls matchOne(Symbol)
	 * repeatedly, however some data structures may have a more
	 * efficient implementation.
	 */
	TrieGrammar<Symbol,Result> matchPrefix(List<Symbol> symbols);
	
	
	/**
	 * Returns whether matchOne(Symbol) could succeed for any symbol
	 */
	boolean hasExtensions();
	
	
	/**
	 * Returns whether the current node/state is a "final state" that has results
	 */
	boolean hasResults();
	
	
	/** 
	 * Retrieve an iterator over the results at the current
	 * node/state. The implementation of this method must adhere
	 * to the following laws:
	 *
	 * (1) The return value is always non-null. It may be an
	 *     empty iterator however.
	 * (2) The iterator must be empty if hasResults() is false,
	 *     and must be non-empty if hasResults() is true.
	 * (3) The iterator must be sorted (at least as used by
	 *     TMGrammar)
	 */
	Iterator<Result> getResults();
}
