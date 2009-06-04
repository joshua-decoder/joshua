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
 * Indicates that a child cannot be added to a parent node, because
 * it already exists.
 *
 * @author Lane Schwartz
 */
public class ChildNodeAlreadyExistsException extends RuntimeException {

	/** Serialization identifier. */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs a new exception indicating that a child cannot
	 * be added to a parent node, because it already exists.
	 * 
	 * @param parent A node in a prefix tree
	 * @param childID Integer identifier of a child node
	 */
	public ChildNodeAlreadyExistsException(Node parent, int childID) {
		super("Child " + childID + " already exists in node " + parent);
	}
	
}
