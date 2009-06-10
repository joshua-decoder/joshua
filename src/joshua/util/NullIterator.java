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
package joshua.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * This class provides a null-object Iterator. That is, an iterator
 * over an empty collection.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class NullIterator<E> implements Iterable<E>, Iterator<E> {
	
//===============================================================
// Iterable -- for foreach loops, because sometimes Java can be very stupid
//===============================================================
	
	/**
	 * Return self as an iterator. We restrict the return type
	 * because some code is written to accept both Iterable and
	 * Iterator, and the fact that we are both confuses Java.
	 * So this is just an upcast, but more succinct to type.
	 */
	public Iterator<E> iterator() { return this; }
	
	
//===============================================================
// Iterator
//===============================================================
	
	/** Always returns false. */
	public boolean hasNext() { return false; }
	
	/** Always throws {@link NoSuchElementException}. */
	public E next() throws NoSuchElementException {
		throw new NoSuchElementException();
	}
	
	/** Unsupported. */
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
