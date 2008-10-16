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
package joshua.lattice;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Node class.
 * 
 * @author Lane Schwartz
 * @since 2008-07-09
 * @version $LastChangedDate$
 */
@Test(groups = { "lattice_node" })
public class NodeTest {

	private final int id = 12345;
	
	private Node<String> node;
	
	@Test
	public void constructNode() {

		node = new Node<String>(id);
		
		Assert.assertEquals((int) node.id, (int) id);
		Assert.assertTrue(node.outgoingArcs.isEmpty());
		Assert.assertEquals(node.size(), 0);
		
	}
	
	
	@Test(dependsOnMethods = { "constructNode" })
	public void getNumber() {
		
		Assert.assertEquals(node.getNumber(), id);
		
	}
	
	
	@Test(dependsOnMethods = { "constructNode" })
	public void toStringTest() {
		
		Assert.assertEquals(node.toString(), "Node-"+id);
		
	}
	
	
	@Test(dependsOnMethods = { "constructNode", "joshua.lattice.ArcTest.constructArc" })
	public void addArc() {
		
		Node<String> n2 = new Node<String>(2);
		double w2 = 0.123;
		String l2 = "somthing cool";
		
		Node<String> n3 = new Node<String>(3);
		double w3 = 124.78;
		String l3 = "hurray!";
		
		Node<String> n4 = new Node<String>(4);
		double w4 = Double.POSITIVE_INFINITY;
		String l4 = "\u0000";
		
		Assert.assertEquals(node.size(), 0);
		
		node.addArc(n2, w2, l2);
		Assert.assertEquals(node.size(), 1);
		Arc<String> a2 = node.outgoingArcs.get(0);
		Assert.assertEquals(a2.head, node);
		Assert.assertEquals(a2.tail, n2);
		Assert.assertEquals(a2.cost, w2);
		Assert.assertEquals(a2.label, l2);
		
		node.addArc(n3, w3, l3);
		Assert.assertEquals(node.size(), 2);
		Arc<String> a3 = node.outgoingArcs.get(1);
		Assert.assertEquals(a3.head, node);
		Assert.assertEquals(a3.tail, n3);
		Assert.assertEquals(a3.cost, w3);
		Assert.assertEquals(a3.label, l3);
		
		node.addArc(n4, w4, l4);
		Assert.assertEquals(node.size(), 3);
		Arc<String> a4 = node.outgoingArcs.get(2);
		Assert.assertEquals(a4.head, node);
		Assert.assertEquals(a4.tail, n4);
		Assert.assertEquals(a4.cost, w4);
		Assert.assertEquals(a4.label, l4);
		
	}
}
