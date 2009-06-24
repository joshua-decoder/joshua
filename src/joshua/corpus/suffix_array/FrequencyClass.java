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
package joshua.corpus.suffix_array;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Represents a term frequency class in the context of 
 * Yamamoto & Church (2001).
 *
 * @author Lane Schwartz
 */
public class FrequencyClass {
	
	/** 
	 * Inclusive start index of an interval. 
	 * 
	 * This index is an index into a suffix array. 
	 */
	private final int i;
	
	/** 
	 * Inclusive end index of an interval.
	 * 
	 * This index is an index into a suffix array. 
	 */
	private final int j;
	
	/** 
	 * Representative index into a suffix array.
	 * <p>
	 * For an interval that is lcp-delimited,
	 * the interval is uniquely determined by a representative index k
	 * such that i < k <= j, and lcp[k] is the shortest interior lcp
	 * for the interval.
	 */
	private final int k;
	
	/** 
	 * Term frequency of each terms that is a member 
	 * of the class defined by the interval <i,j>.
	 */
	private final int frequency;

	private final int[] longestCommonPrefixes;
	
	
	/**
	 * Constructs a non-trivial frequency record.
	 * 
	 * @param i Inclusive start index of an interval.
	 *          This index is an index into a suffix array.
	 * @param j Inclusive end index of an interval.
	 *          This index is an index into a suffix array.
	 * @param k Representative index into a suffix array.
	 *          For an interval that is lcp-delimited,
	 *          the interval is uniquely determined by 
	 *          a representative index k such that i < k <= j, 
	 *          and lcp[k] is the shortest interior lcp
	 *          for the interval.
	 * @param frequency Term frequency of each terms that is a member 
	 *                  of the class defined by the interval <i,j>.
	 * @param longestCommonPrefixes Longest common prefix array
	 */
	public FrequencyClass(int i, int j, int k, int frequency, int[] longestCommonPrefixes) {
		this.i = i;
		this.j = j;
		this.k = k;
		this.frequency = frequency;
		this.longestCommonPrefixes = longestCommonPrefixes;
	}
	
	/**
	 * Constructs a trivial frequency record.
	 * 
	 * @param j Inclusive start and end index of an interval.
	 *          This index is an index into a suffix array.
	 * @param longestCommonPrefixes Longest common prefix array
	 */
	public FrequencyClass(int j, int[] longestCommonPrefixes) {
		this.i = j;
		this.j = j;
		this.k = -1;
		this.frequency = 1;
		this.longestCommonPrefixes = longestCommonPrefixes;
	}
	
	public boolean hasTrivialInterval() {
		return i==j;
	}
	
	/**
	 * Gets the inclusive start index of the interval.
	 * 
	 * @return The inclusive start index of the interval
	 */
	public int getIntervalStart() {
		return this.i;
	}
	
	/**
	 * Gets the inclusive end index of the interval.
	 * 
	 * @return The inclusive end index of the interval
	 */
	public int getIntervalEnd() {
		return this.j;
	}
	
	/**
	 * Gets the representative index into a suffix array.
	 *          For an interval that is lcp-delimited,
	 *          the interval is uniquely determined by 
	 *          a representative index k such that i < k <= j, 
	 *          and lcp[k] is the shortest interior lcp
	 *          for the interval.
	 *          
	 * @return the representative index into a suffix array.
	 *          For an interval that is lcp-delimited,
	 *          the interval is uniquely determined by 
	 *          a representative index k such that i < k <= j, 
	 *          and lcp[k] is the shortest interior lcp
	 *          for the interval.
	 */
	public int getRepresentativeIndex() {
		return this.k;
	}
	
	/**
	 * Gets the term frequency of each terms that is a member 
	 *                  of the class defined by the interval <i,j>.
	 *                  
	 * @return The term frequency of each terms that is a member 
	 *                  of the class defined by the interval <i,j>.
	 */
	public int getFrequency() {
		return this.frequency;
	}
	
	public Iterable<Integer> validPhraseLengths(final int maxPhraseLength) {
		final int longestBoundingLCP = Math.max(longestCommonPrefixes[i], longestCommonPrefixes[j+1]);
		final int shortestInteriorLCP = (hasTrivialInterval()) ? Integer.MAX_VALUE : longestCommonPrefixes[k];
		
		return new Iterable<Integer>() {

			public Iterator<Integer> iterator() {
				
				return new Iterator<Integer>() {
					
					int max = Math.min(maxPhraseLength, shortestInteriorLCP);
					int m = longestBoundingLCP+1;
					
					public boolean hasNext() {
						return m <= max;
					}

					public Integer next() {
						int next = m;
						m += 1;
						return next;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}

	public boolean equals(Object o) {
		if (o instanceof FrequencyClass) {
			FrequencyClass other = (FrequencyClass) o;
			
			if (this.i==other.i && 
					this.j==other.j && 
					this.k==other.k && 
					this.frequency==other.frequency && 
					Arrays.equals(
							this.longestCommonPrefixes, 
							other.longestCommonPrefixes)) {
				return true;
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}

	
	public int hashCode() {
		return frequency + 37*i + 71*j + 83*k + 
			Arrays.hashCode(longestCommonPrefixes);
	}
	
	public String toString() {
		if (hasTrivialInterval()) {
			return "trivial <"+i+","+j+">, tf="+frequency;
		} else {
			return "nontrivial <"+i+","+j+"> rep="+k+", tf="+frequency;
		}
	}
}
