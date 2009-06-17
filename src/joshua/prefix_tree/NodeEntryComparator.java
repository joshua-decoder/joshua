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
package joshua.prefix_tree;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 * @author Lane Schwartz
 */

class NodeEntryComparator implements Comparator<Map.Entry<Integer, Node>> {

	/** Serializable identifier. */
	private static final long serialVersionUID = 1L;
	
	private static final NodeEntryComparator comparator 
		= new NodeEntryComparator();
	
	private NodeEntryComparator() {}
	
	public static NodeEntryComparator get() {
		return comparator;
	}
	
	public int compare(Entry<Integer, Node> o1, Entry<Integer, Node> o2) {
		return o1.getValue().compareTo(o2.getValue());
	}
	
}
