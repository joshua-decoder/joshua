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

/**
 * Represents a span with an inclusive starting index and an exclusive
 * ending index.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class Span implements Iterable<Integer>, Comparable<Span> {

	/** Inclusive starting index of this span. */
	public int start;
	
	/** Exclusive ending index of this span. */
	public int end;
	
	
	/**
	 * Constructs a new span with the given inclusive starting
	 * and exclusive ending indices.
	 *
	 * @param start Inclusive starting index of this span.
	 * @param end Exclusive ending index of this span.
	 */
	public Span(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	
	/**
	 * Returns the length of the span.
	 * 
	 * @return the length of the span; this is equivalent to
	 *         <code>span.end - span.start</code>.
	 */
	public int size() {
		return end-start;
	}
	
	
	public String toString() {
		return "["+start+"-"+end+")";
	}
	
	
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {

			int next = start;
			
			public boolean hasNext() {
				if (next < end) {
					return true;
				} else {
					return false;
				}
			}

			public Integer next() {
				return next++;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	
	public int compareTo(Span o) {
		
		if (o==null) {
			throw new NullPointerException();
		} else {

			if (start < o.start) {
				return -1;
			} else if (start > o.start) {
				return 1;
			} else {
				if (end < o.end) {
					return -1;
				} else if (end > o.end) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) {
			return true;
		} else if (o instanceof Span) {
			Span other = (Span) o;
			return (start == other.start && end == other.end);
			
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() { 

		return start*31 + end*773;

	}

	
}
