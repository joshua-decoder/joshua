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

// Imports
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache is a class that implements a least recently used cache.  It is a 
 * straightforward extension of java.util.LinkedHashMap with its 
 * removeEldestEntry method overridden, so that stale entries are deleted
 * once we reach the specified capacity of the Cache.
 *
 * This class is quite useful for storing the results of computations that 
 * we would do many times over in the FeatureFunctions.
 *
 * @author Chris Callison-Burch
 * @since  14 April 2005
 *
 */
// ccb - todo - figure out how to type the keys and values here...
public class Cache<K,V> extends LinkedHashMap<K,V> {

//===============================================================
// Constants
//===============================================================

	/** A constant is used as the default the cache size if none
	  * is specified.
	  */
	public static final int DEFAULT_CAPACITY = 100000;

//===============================================================
// Member variables
//===============================================================

	int maxCapacity;
 
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Creates a Cache with a set capacity. 
	 *
	 * @param maxCapacity the maximum capacity of the cache.
	 */
	public Cache(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}


	/**
	 * Creates a Cache with the DEFAULT_CAPACITY.
	 */
	public Cache() {
		this(DEFAULT_CAPACITY);
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	
	//===========================================================
	// Methods
	//===========================================================



//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

    /**
     * This method is invoked by put and putAll after inserting a new entry
	 * into the map. Once we reach the capacity of the cache, we remove the
	 * oldest entry each time a new entry is added. This reduces memory 
	 * consumption by deleting stale entries.
	 *
     * @param eldest the eldest entry
     * @return true if the capacity is greater than the maximum capacity
     */
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxCapacity;
    }

//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args)
	{

	}
}

