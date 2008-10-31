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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Lattice class.
 * 
 * @author Lane Schwartz
 * @since 2008-07-09
 * @version $LastChangedDate$
 */
@Test(groups = { "lattice" })
public class LatticeTest {

	@Test
	public void allPairsShortestPath() {

		List<Node<String>> nodes = new ArrayList<Node<String>>();
		for (int i=0; i<4; i++) {
			nodes.add(new Node<String>(i));
		}
		
		nodes.get(0).addArc(nodes.get(1), 1.0, "x");
		nodes.get(1).addArc(nodes.get(2), 1.0, "y");
		nodes.get(0).addArc(nodes.get(2), 1.5, "a");
		nodes.get(2).addArc(nodes.get(3), 3.0, "b");
		nodes.get(2).addArc(nodes.get(3), 5.0, "c");
		
		Lattice<String> graph = new Lattice<String>(nodes);
		
		Assert.assertEquals(graph.getShortestPath(0, 1), 1.0);
		Assert.assertEquals(graph.getShortestPath(0, 2), 1.0);
		Assert.assertEquals(graph.getShortestPath(1, 2), 1.0);
		Assert.assertEquals(graph.getShortestPath(0, 3), 2.0);
		Assert.assertEquals(graph.getShortestPath(1, 3), 2.0);
		Assert.assertEquals(graph.getShortestPath(2, 3), 1.0);
	}
	
	@Test
	public void createFromString() {

		String data = 
			
			// Start of lattice
			"("+
			
				// Node 0
				"("+
					"('A',1.0,5),"+ // Arc with label A and cost 1.0. Destination is Node 5 (Node 0 + span of 5)  
					"('B',1.0,2),"+ // Arc with label B and cost 1.0. Destination is Node 2 (Node 0 + span of 2)
					"('C',1.0,3),"+ // Arc with label C and cost 1.0. Destination is Node 3 (Node 0 + span of 3)
					"('D',1.0,1),"+ // Arc with label D and cost 1.0. Destination is Node 1 (Node 0 + span of 1)
				")," +
				
				// Node 1
				"(" +
					"('E',1.0,4)," + // Arc with label E and cost 1.0. Destination is Node 5 (Node 1 + span of 4)
				")," +
				
				// Node 2
				"(" +
					"('C',1.0,3)," + // Arc with label C and cost 1.0. Destination is Node 5 (Node 2 + span of 3)
				")," +
				
				// Node 3
				"(" +
					"('D',1.0,1)," + // Arc with label D and cost 1.0. Destination is Node 4 (Node 3 + span of 1)
				")," +
				
				// Node 4
				"(" +
					"('E',1.0,1)," + // Arc with label E and cost 1.0. Destination is Node 5 (Node 4 + span of 1)
				")," +
				
				// Node 5
				"(" +
					"('X',1.0,1)," + // Arc with label X and cost 1.0. Destination is Node 6 (Node 5 + span of 1)
				")," +
				
				// There is an implicit final state (Node 6).
				
			")"; // End of lattice
		
		
		Lattice<String> lattice = Lattice.createFromString(data);
		
		int numberOfNodes = 7;
		
		Assert.assertEquals(lattice.size(), numberOfNodes);
		
		Node<String> node0 = lattice.getNode(0);
		Node<String> node1 = lattice.getNode(1);
		Node<String> node2 = lattice.getNode(2);
		Node<String> node3 = lattice.getNode(3);
		Node<String> node4 = lattice.getNode(4);
		Node<String> node5 = lattice.getNode(5);
		Node<String> node6 = lattice.getNode(6);
		
		Assert.assertEquals(node0.size(), 4);
		Assert.assertEquals(node1.size(), 1);
		Assert.assertEquals(node2.size(), 1);
		Assert.assertEquals(node3.size(), 1);
		Assert.assertEquals(node4.size(), 1);
		Assert.assertEquals(node5.size(), 1);
		Assert.assertEquals(node6.size(), 0);
		
		
		// Node 0 outgoing arcs
		
		Arc<String> arcA_0_5 = node0.outgoingArcs.get(0);
		Assert.assertEquals(arcA_0_5.getLabel(), "A");
		Assert.assertEquals(arcA_0_5.getHead(), node0);
		Assert.assertEquals(arcA_0_5.getTail(), node5);
		Assert.assertEquals(arcA_0_5.getCost(), 1.0);
		
		Arc<String> arcB_0_2 = node0.outgoingArcs.get(1);
		Assert.assertEquals(arcB_0_2.getLabel(), "B");
		Assert.assertEquals(arcB_0_2.getHead(), node0);
		Assert.assertEquals(arcB_0_2.getTail(), node2);
		Assert.assertEquals(arcB_0_2.getCost(), 1.0);		
		
		Arc<String> arcC_0_3 = node0.outgoingArcs.get(2);
		Assert.assertEquals(arcC_0_3.getLabel(), "C");
		Assert.assertEquals(arcC_0_3.getHead(), node0);
		Assert.assertEquals(arcC_0_3.getTail(), node3);
		Assert.assertEquals(arcC_0_3.getCost(), 1.0);	
		
		Arc<String> arcD_0_1 = node0.outgoingArcs.get(3);
		Assert.assertEquals(arcD_0_1.getLabel(), "D");
		Assert.assertEquals(arcD_0_1.getHead(), node0);
		Assert.assertEquals(arcD_0_1.getTail(), node1);
		Assert.assertEquals(arcD_0_1.getCost(), 1.0);
		
		
		// Node 1 outgoing arcs
		Arc<String> arcE_1_5 = node1.outgoingArcs.get(0);
		Assert.assertEquals(arcE_1_5.getLabel(), "E");
		Assert.assertEquals(arcE_1_5.getHead(), node1);
		Assert.assertEquals(arcE_1_5.getTail(), node5);
		Assert.assertEquals(arcE_1_5.getCost(), 1.0);
		
		
		// Node 2 outgoing arcs
		Arc<String> arcC_2_5 = node2.outgoingArcs.get(0);
		Assert.assertEquals(arcC_2_5.getLabel(), "C");
		Assert.assertEquals(arcC_2_5.getHead(), node2);
		Assert.assertEquals(arcC_2_5.getTail(), node5);
		Assert.assertEquals(arcC_2_5.getCost(), 1.0);
		
		
		// Node 3 outgoing arcs
		Arc<String> arcD_3_4 = node3.outgoingArcs.get(0);
		Assert.assertEquals(arcD_3_4.getLabel(), "D");
		Assert.assertEquals(arcD_3_4.getHead(), node3);
		Assert.assertEquals(arcD_3_4.getTail(), node4);
		Assert.assertEquals(arcD_3_4.getCost(), 1.0);
		
		
		// Node 4 outgoing arcs
		Arc<String> arcE_4_5 = node4.outgoingArcs.get(0);
		Assert.assertEquals(arcE_4_5.getLabel(), "E");
		Assert.assertEquals(arcE_4_5.getHead(), node4);
		Assert.assertEquals(arcE_4_5.getTail(), node5);
		Assert.assertEquals(arcE_1_5.getCost(), 1.0);
		
		
		// Node 5 outgoing arcs
		Arc<String> arcX_5_6 = node5.outgoingArcs.get(0);
		Assert.assertEquals(arcX_5_6.getLabel(), "X");
		Assert.assertEquals(arcX_5_6.getHead(), node5);
		Assert.assertEquals(arcX_5_6.getTail(), node6);
		Assert.assertEquals(arcX_5_6.getCost(), 1.0);
	}
}
