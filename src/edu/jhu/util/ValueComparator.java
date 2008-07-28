package  edu.jhu.util;

// Imports
import java.util.*;

/**
 * A comparitor that allows us to sort the keys in a Map by their value.
 *
 * @author  Chris Callison-Burch
 * @since  14 August 2004
 *
 * The contents of this file are subject to the Linear B Community Research 
 * License Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.linearb.co.uk/developer/. Software distributed under the License
 * is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either 
 * express or implied. See the License for the specific language governing 
 * rights and limitations under the License. 
 *
 * Copyright (c) 2004-2005 Linear B Ltd. All rights reserved.
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

