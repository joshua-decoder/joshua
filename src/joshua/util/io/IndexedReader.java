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
package joshua.util.io;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.IOException;


/**
 * Wraps a reader with "line" index information.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class IndexedReader<E> implements Reader<E> {
	
	/** A name for the type of elements the reader produces. */
	private final String elementName;
	
	/** The number of elements the reader has delivered so far. */
	private int lineNumber;
	
	/** The underlying reader. */
	private final Reader<E> reader;
	
	public IndexedReader(String elementName, Reader<E> reader) {
		this.elementName = elementName;
		this.lineNumber  = 0;
		this.reader      = reader;
	}
	
	
//===============================================================
// Public (non-interface) methods
//===============================================================
	
	/** Return the number of elements delivered so far. */
	public int index() {
		return this.lineNumber;
	}
	
	
	/**
	 * Wrap an IOException's message with the index when it
	 * occured.
	 */
	public IOException wrapIOException(IOException oldError) {
		IOException newError = new IOException(
			"At " + this.elementName + " " + this.lineNumber +
			": " + oldError.getMessage());
		newError.initCause(oldError);
		return newError;
	}
	
//===============================================================
// Reader
//===============================================================
	
	/** Delegated to the underlying reader. */
	public boolean ready() throws IOException {
		try {
			return this.reader.ready();
		} catch (IOException oldError) {
			throw wrapIOException(oldError);
		}
	}
	
	
	/**
	 * Delegated to the underlying reader. Note that we do not
	 * have a <code>finalize()</code> method; however, when we
	 * fall out of scope, the underlying reader will too, so its
	 * finalizer may be called. For correctness, be sure to
	 * manually close all readers.
	 */
	public void close() throws IOException {
		try {
			this.reader.close();
		} catch (IOException oldError) {
			throw wrapIOException(oldError);
		}
	}
	
	
	/** Delegated to the underlying reader. */
	public E readLine() throws IOException {
		E line;
		try {
			line = this.reader.readLine();
		} catch (IOException oldError) {
			throw wrapIOException(oldError);
		}
		++this.lineNumber;
		return line;
	}


//===============================================================
// Iterable -- because sometimes Java can be very stupid
//===============================================================
	
	/** Return self as an iterator. */
	public Iterator<E> iterator() {
		return this;
	}
	
	
//===============================================================
// Iterator
//===============================================================
	
	/** Delegated to the underlying reader. */
	public boolean hasNext() {
		return this.reader.hasNext();
	}
	
	
	/** Delegated to the underlying reader. */
	public E next() throws NoSuchElementException {
		E line = this.reader.next();
		// Let exceptions out, we'll wrap any errors a closing time.
		
		++this.lineNumber;
		return line;
	}
	
	
	/**
	 * If the underlying reader supports removal, then so do
	 * we. Note that the {@link #index()} method returns the
	 * number of elements delivered to the client, so removing
	 * an element from the underlying collection does not affect
	 * that number.
	 */
	public void remove() throws UnsupportedOperationException {
		this.reader.remove();
	}
}
