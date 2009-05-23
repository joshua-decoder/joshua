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

import java.io.Serializable;
import java.util.*;

/**
 * ReverseOrder is a Comparator that reverses the natural order of
 * Comparable objects.
 *
 * @author Chris Callison-Burch
 * @since  2 June 2008
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class ReverseOrder<K extends Comparable<K>> implements Comparator<K>, Serializable {

//===============================================================
// Public
//===============================================================	

	//===========================================================
	// Methods
	//===========================================================


	public int compare(K obj1, K obj2) {
		int comparison = obj1.compareTo(obj2);
		if(comparison != 0) {
			comparison = comparison * -1;
		} 
		return comparison;
	}
	
	

//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

}

