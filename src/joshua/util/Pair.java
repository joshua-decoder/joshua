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
 * Represents a pair of elements.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 *
 * @param <First> Type of the first element in the pair.
 * @param <Second> Type of the second element in the pair.
 */
public class Pair<First, Second> {

	/** The first element of the pair. */
	public First first;
	
	/** The second element of the pair. */
	public Second second;
	
	private Integer hashCode = null;
	
	/**
	 * Constructs a pair of elements.
	 * 
	 * @param first the first element in the pair
	 * @param second the second element in the pair
	 */
	public Pair(First first, Second second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Gets the second element in the pair
	 * 
	 * @return the first element in the pair
	 */
	public First getFirst() {
		return first;
	}

	/**
	 * Sets the first element in the pair.
	 * 
	 * @param first the new value for the first element in the pair
	 */
	public void setFirst(First first) {
		this.first = first;
	}

	/**
	 * Gets the second element in the pair.
	 * 
	 * @return the second element in the pair
	 */
	public Second getSecond() {
		return second;
	}

	/**
	 * Sets the second element in the pair.
	 * 
	 * @param second the new value for the second element in the pair
	 */
	public void setSecond(Second second) {
		this.second = second;
	}
	
	
	public int hashCode() {
		
		if (hashCode==null) {
			if (first==null) {
				if (second==null) {
					hashCode = 0;
				} else {
					hashCode = second.hashCode();
				}
			} else if (second==null) {
				hashCode = first.hashCode();
			} else {
				hashCode = first.hashCode() + 37*second.hashCode();
			}
		}
		
		return hashCode;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			
			Pair other = (Pair) o;
			
			if (first==null) {
				if (second==null) {
					return other.first==null && other.second==null;
				} else {
					return other.first==null && second.equals(other.second);
				}
			} else if (second==null) {
				return first.equals(other.first) && other.second==null;
			} else {
				return first.equals(other.first) && second.equals(other.second);
			}
			
		} else {
			return false;
		}
	}
	
}
