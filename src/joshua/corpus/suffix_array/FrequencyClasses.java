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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents a list of term frequency classes in the context of 
 * Yamamoto & Church (2001).
 *
 * @author Lane Schwartz
 */
public class FrequencyClasses implements Iterable<FrequencyClass> {
	
	final ArrayList<Integer> data;
	
	final int[] longestCommonPrefixes;
	
	int numClasses;
	int numTrivialClasses;
	
	/**
	 * Constructs an initially empty list of frequency class data.
	 */
	public FrequencyClasses(int[] longestCommonPrefixes) {
		this.data = new ArrayList<Integer>();
		this.longestCommonPrefixes = longestCommonPrefixes;
		this.numClasses = 0;
		this.numTrivialClasses = 0;
	}
	
	/**
	 * Record a term frequency class.
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
	 */
	public void record(int i, int j, int k, int frequency) {
		data.add(i);
		data.add(j);
		data.add(k);
		data.add(frequency);
		
		numClasses++;
	}

	/**
	 * Record a trivial term frequency class.
	 * 
	 * @param j Inclusive start and end index of an interval.
	 *          This index is an index into a suffix array.
	 */
	public void record(int j) {
		data.add(j);
		data.add(j);
		
		numTrivialClasses++;
	}
	
	
	/**
	 * Gets the number of frequency classes
	 * stored in this object.
	 * 
	 * @return The number of frequency classes
	 *         stored in this object.
	 */
	public int size() {
		return numClasses + numTrivialClasses;
	}
	
	/**
	 * Gets an iterator capable of traversing
	 * all term frequency classes recorded by this object.
	 * 
	 * @return An iterator capable of traversing
	 *         all term frequency classes recorded by this object
	 */
	public Iterator<FrequencyClass> iterator() {
		return this.withMinimumFrequency(0).iterator();
	}
	
	
	public Iterable<FrequencyClass> withMinimumFrequency(final int minFrequency) {
		return new Iterable<FrequencyClass>() {

			public Iterator<FrequencyClass> iterator() {
				return new Iterator<FrequencyClass>() {

					int index=0;
					FrequencyClass next = null;
					
					public boolean hasNext() {
						
						boolean hasNext = false;
						
						while (index<data.size()) {
							
							int i = data.get(index++);
							int j = data.get(index++);
							
							if (i==j) {
								if (minFrequency <= 1) {
									next = new FrequencyClass(j, longestCommonPrefixes);
									hasNext = true;
									break;
								}
							} else {
								int k = data.get(index++);
								int frequency = data.get(index++);
								
								if (frequency >= minFrequency) {
									next = new FrequencyClass(i, j, k, frequency, longestCommonPrefixes);
									hasNext = true;
									break;
								}
							}
							
						}
												
						return hasNext;
					}

					public FrequencyClass next() {
						
						if (next==null) {
							if (hasNext()) {
								return next;
							} else {
								throw new NoSuchElementException();
							}
						} else {
							return next;
						}
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
		

	}
	
}
