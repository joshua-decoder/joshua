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

import java.util.Comparator;

/**
 * Represents an object being counted,
 * with the associated count.
 *
 * @author Lane Schwartz
 */
public class Counted<E> implements Comparable<Counted<E>> {

	/** The element being counted. */
	private final E element;

	/** The count associated with the element. */
	private final Integer count;
	
	/**
	 * Constructs an object wrapping 
	 * an element and its associated count.
	 * 
	 * @param element An element being counted
	 * @param count The count associated with the element
	 */
	public Counted(E element, int count) {
		this.element = element;
		this.count = count;
	}
	
	/**
	 * Gets the count associated with
	 * this object's element.
	 * 
	 * @return The count associated with
	 *         this object's element
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Gets the element associated with this object.
	 * 
	 * @return The element associated with this object
	 */
	public E getElement() {
		return element;
	}

	/**
	 * Compares this object to another counted object,
	 * according to the natural order of the counts
	 * associated with each object.
	 * 
	 * @param o Another counted object
	 * @return -1 if the count of this object 
	 *          is less than the count of the other object,
	 *          0 if the counts are equal, or
	 *          1 if the count of this object
	 *          is greater than the count of the other object
	 */
	public int compareTo(Counted<E> o) {
		return count.compareTo(o.count);
	}
	
	/**
	 * Gets a comparator that compares two counted objects
	 * based on the reverse of the natural order of the counts
	 * associated with each object.
	 * 
	 * @param <E>
	 * @return A comparator that compares two counted objects
	 *         based on the reverse of the natural order of the counts
	 *         associated with each object
	 */
	public static <E> Comparator<Counted<E>> getDescendingComparator() { 
		return new Comparator<Counted<E>>() {
			public int compare(Counted<E> o1, Counted<E> o2) {
				return (o2.count.compareTo(o1.count));
			}
		};
	}
}
