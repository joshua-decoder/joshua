/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.util;

// Imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Counts is the class that increments and decrements
 * the number of times an object is seen.
 *
 * @author Chris Callison-Burch
 * @since  4 January 2005
 * @version $LastChangedDate$
 */
public class Counts {

//===============================================================
// Constants
//===============================================================
	
//===============================================================
// Member variables
//===============================================================

	private HashMap counts;
	private int totalCount;

//===============================================================
// Constructor(s)
//===============================================================

	public Counts() {
		counts = new HashMap();
		totalCount = 0;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/** Returns the count for the object.
	  */
	public int getCount(Object obj) {
		return getCount(counts, obj);
	}
	
	
	/** Returns the collective total of all of the counts
	  * of all of the objects stored in then Counts.
	  */
	public int getTotalCount() {
		return totalCount;
	}
	
	/** Returns the number of unique objects in the Counts. 
	  */
	public int getNumObjects() {
		return counts.keySet().size();
	}
	
	
	/** Increments the count for the object by 1. 
	  */
	public void incrementCount(Object obj) {
		incrementCount(obj, 1);
	}
	
	/** Increments the count for the object by the specified amount. 
	  */
	public void incrementCount(Object obj, int amount) {
		incrementCount(counts, obj, amount);
		totalCount += amount;
	}
	
	
	/** Increments the counts for all objects in the collection. 
	  */
	public void incrementAll(Collection c) {
		if(c != null) {
			Iterator it = c.iterator();
			while(it.hasNext()) {
				incrementCount(it.next());
			}
		}
	}
	
	
	/** Returns a collection of the objects in the Counts.
	  */
	public ArrayList keys() {
		ArrayList keys = new ArrayList();
		keys.addAll(counts.keySet());
		return keys;
	}
	
	/** Returns a collection of the objects in the Counts, sorted by
	  * their values.
	  */
	public ArrayList keys(boolean sortAscending) {
		ArrayList keys = new ArrayList();
		keys.addAll(counts.keySet());
		Collections.sort(keys, new ValueComparator(counts, sortAscending));
		return keys;
	}
	
	
	/** Returns an iterator over the objects in the Counts.
	  */
	public Iterator iterator() {
		return counts.keySet().iterator();
	}
	
	
	/** Returns an iterator over the Counts objects,
	  * sorted by their frequency.
	  */
	public Iterator iterator(boolean sortAscending) {
		ArrayList keys = new ArrayList();
		keys.addAll(counts.keySet());
		Collections.sort(keys, new ValueComparator(counts, sortAscending));
		return keys.iterator();
	}
	
	//===========================================================
	// Methods
	//===========================================================

	/** Adds the counts of the specified counts to this one. 
	  */
	public void merge(Counts counts) {
		Iterator it = counts.iterator();
		while(it.hasNext()) {
			Object obj = it.next();
			int count = counts.getCount(obj);
			this.incrementCount(obj, count);
		}
	}

	/** Prints a sorted list of the objects and their counts, allows
	  * the order to be specified as asending or decending.
	  */	
	public String toString(boolean sortAscending) {
		ArrayList keys = new ArrayList();
		keys.addAll(counts.keySet());
		
		if(keys.size() > 0) {
			Collections.sort(keys, new ValueComparator(counts, sortAscending));
			Object obj = keys.get(0);
			StringBuffer buffer = new StringBuffer("[" + obj.toString() + " = " + getCount(obj));
			for(int i = 1; i < keys.size(); i++) {
				obj = keys.get(i);
				buffer.append(", " + obj.toString() + " = " + getCount(obj));
			}
			buffer.append("]");
			return buffer.toString();
		} else {
			return ("[ ]");
		}
	}
	
	/** Prints a list of the objects and their counts, and sorts them
	  * by count in decending order.
	  */
	public String toString() {
		return toString(false);
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================



//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================

	private static int getCount(Map hash, Object obj) {
		Integer count = (Integer) hash.get(obj);
		if(count == null) return 0;
		return count.intValue();
	}
	
	
	private static void incrementCount(Map hash, Object obj) {
		incrementCount(hash, obj, 1);
	}
	
	private static void incrementCount(Map hash, Object obj, int amount) {
		int count = getCount(hash, obj);
		count += amount;
		hash.put(obj, new Integer(count));
	}	
	
	
	private static HashMap mergeCounts(Map hash1, Map hash2) {
		HashMap newMap = new HashMap();
		newMap.putAll(hash1);
		Iterator it = hash2.keySet().iterator();
		while(it.hasNext()) {
			Object obj = it.next();
			int count = getCount(hash2, obj);
			incrementCount(newMap, obj, count);
		}
		return newMap;
	}


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args)
	{

	}
}

