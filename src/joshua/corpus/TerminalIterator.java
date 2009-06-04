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
package joshua.corpus;

import java.util.Iterator;
import java.util.NoSuchElementException;

import joshua.corpus.vocab.SymbolTable;

/**
 * Iterator capable of iterating over those word identifiers
 * in a phrase which represent terminals.
 * <p>
 * <em>Note</em>: This class is <em>not</em> thread-safe.
 * 
 * @author Lane Schwartz
 */
public class TerminalIterator implements Iterator<Integer> {
	
	private final int[] words;
	private final SymbolTable vocab;
	
	private int nextIndex = -1;
	private int next = Integer.MIN_VALUE;
	private boolean dirty = true;
	
	/**
	 * Constructs an iterator for the terminals
	 * in the given list of words.
	 * 
	 * @param vocab 
	 * @param words
	 */
	public TerminalIterator(SymbolTable vocab, int[] words) {
		this.vocab = vocab;
		this.words = words;
	}
	
	/* See Javadoc for java.util.Iterator#next(). */
	public boolean hasNext() {
		
		while (dirty || vocab.isNonterminal(next)) {
			nextIndex++;
			if (nextIndex < words.length) {
				next = words[nextIndex];
				dirty = false;
			} else {
				return false;
			}
		}
		
		return true;
	}

	/* See Javadoc for java.util.Iterator#next(). */
	public Integer next() {
		if (hasNext()) {
			dirty = true;
			return next;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Unsupported operation, 
	 * guaranteed to throw an UnsupportedOperationException.
	 * 
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
}