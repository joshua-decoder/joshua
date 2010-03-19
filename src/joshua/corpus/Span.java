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

import java.util.List;
import java.util.ArrayList;

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

	/**
	 * Returns all subspans of the given Span.
	 *
	 * @return a list of all subspans.
	 */
	public List<Span> getSubSpans()
	{
		return getSubSpans(size());
	}

	/**
	 * Returns all subspans of the given Span, up to a specified Span size.
	 *
	 * @param max the maximum Span size to return
	 * @return a list all subspans up to the given size
	 */
	public List<Span> getSubSpans(int max)
	{
		int spanSize = size();
		ArrayList<Span> result = new ArrayList<Span>(max * spanSize);
		for (int len = max; len > 0; len--) {
			for (int i = start; i < end - len + 1; i++) {
				result.add(new Span(i, i + len));
			}
		}
		return result;
	}

	public boolean strictlyContainedIn(Span o)
	{
		return (start >= o.start) && (end <= o.end) && !(start == o.start && end == o.end);
	}
	
	public boolean disjointFrom(Span o)
	{
		if (start < o.start) {
			return end < o.start;
		}
		if (end > o.end) {
			return start > o.end;
		}
		return false;
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
