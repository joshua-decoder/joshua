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
package  edu.jhu.util;

// Imports
import java.util.*;

/**
 * A comparitor that allows us to sort the keys in a Map by their value.
 *
 * @author  Chris Callison-Burch
 * @since  14 August 2004
 * @version $LastChangedDate$
 */
public class ValueComparator implements Comparator {

//===============================================================
// Constants
//===============================================================

//===============================================================
// Member variables
//===============================================================

	public Map map;
	private int direction;
	
//===============================================================
// Constructor(s)
//===============================================================
	
	
	public ValueComparator(Map map) {
		this.map = map;
		direction = 1;
		
	}
	
	
	public ValueComparator(Map map, boolean ascend) {
		this.map = map;
		if(ascend) {
			direction = 1;
		} else {
			direction = -1;
		}
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public int compare(Object o1, Object o2) {
		Comparable value1 = (Comparable) map.get(o1);
		Comparable value2 = (Comparable) map.get(o2);
		
		if(value1 == null) {
			return 1 * direction;
		}
		
		if(value2 == null) {
			return -1 * direction;
		}
		
		return (value1.compareTo(value2) * direction);
	}
	
	
	public boolean equals(Object obj) {
		return obj.getClass().equals(this.getClass());
	}	
	
	public void setMap(Map map) {
		this.map = map;
	}
	
	//===========================================================
	// Methods
	//===========================================================
	

	
	
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


	/** 
	 * @return an iterator over that is sorted by the keys in the map.
	 */
	 static public Iterator iterator(Map map, boolean sortAsending) {
		ArrayList keys = new ArrayList();
		keys.addAll(map.keySet());
		Collections.sort(keys, new ValueComparator(map, sortAsending));
		return keys.iterator();
	}

//===============================================================
// Main 
//===============================================================

	public static void main(String[] args)
	{
	
	}
}

