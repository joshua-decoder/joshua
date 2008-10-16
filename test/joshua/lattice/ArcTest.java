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
 * Unit tests for Arc class.
 * 
 * @author Lane Schwartz
 * @since 2008-07-09
 * @version $LastChangedDate$
 */
@Test(groups = { "lattice_arc" })
public class ArcTest {

	private final Node<String> head = new Node<String>(1);
	private final Node<String> tail = new Node<String>(2);
	private final double cost = Math.PI;
	private final String label = "pi";
	
	private Arc<String> arc;
	
	@Test(dependsOnMethods = { "joshua.lattice.NodeTest.constructNode" })
	//@Test(dependsOnGroups = {"lattice_node" })
	public void constructArc() {

		arc = new Arc<String>(head, tail, cost, label);
		
		Assert.assertEquals(arc.head, head);
		Assert.assertEquals(arc.tail, tail);
		Assert.assertEquals(arc.cost, cost);
		Assert.assertEquals(arc.label, label);
		
	}
	
	@Test(dependsOnMethods = { "constructArc" })
	public void getHead() {
		
		Assert.assertEquals(arc.getHead(), head);
		
	}
	
	
	@Test(dependsOnMethods = { "constructArc" })
	public void getTail() {
		
		Assert.assertEquals(arc.getTail(), tail);
		
	}
	
	
	@Test(dependsOnMethods = { "constructArc" })
	public void getCost() {
		
		Assert.assertEquals(arc.getCost(), cost);
		
	}
	
	
	@Test(dependsOnMethods = { "constructArc" })
	public void getLabel() {
		
		Assert.assertEquals(arc.getLabel(), label);
		
	}
}
