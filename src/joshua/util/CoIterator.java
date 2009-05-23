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

/**
 * This interface defines the dual of an {@link java.util.Iterator}.
 *
 * In the plain case some class would have a method that returns
 * an iterator: <code>Iterator&lt;E&gt; method()</code>, and then
 * client code would use <code>E next()</code> to enumerate all
 * elements. In the dual case we pass a co-iterator to the method:
 * <code>void method(CoIterator&lt;E&gt; coit)</code>, and then the
 * method will call <code>coNext(E)</code> for each element. This
 * defines co-routines in order to consume the stream of <code>E</code>
 * as it is being created.
 */
public interface CoIterator<E> {
	
	/**
	 * The dual of <code>Iterator&lt;E&gt;.next</code>. This
	 * method is called for each element of the stream being
	 * consumed.
	 */
	void coNext(E elem);
	
	/**
	 * This method is called once the stream has terminated
	 * or in the event the stream is interrupted by an (unchecked)
	 * exception, in case any cleanup needs to be done. Clients
	 * that do not ensure this method is called are considered
	 * buggy.
	 */
	void finish();
}
