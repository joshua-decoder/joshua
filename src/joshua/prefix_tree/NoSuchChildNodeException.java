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

/**
 * Indicates that a node in the prefix tree does not have the
 * expected child.
 * 
 * @author Lane Schwartz
 */
public class NoSuchChildNodeException extends RuntimeException {

	/** Serializable identifier. */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs a new exception indicating that a child of a
	 * parent node was expected, but does not exist.
	 * 
	 * @param parent A node in a prefix tree
	 * @param childID Integer identifier of an expected child node
	 */
	public NoSuchChildNodeException(Node parent, int childID) {
		super("No child " + childID + " for node " + parent.suffixLink + " (Parent was " + parent + ")");
	}
	
}
