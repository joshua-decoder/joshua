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
package joshua.prefix_tree;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for PrefixTree.Node
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class PrefixTreeNodeTest {

	//TODO Implement test case for PrefixTree.Node.translate()
	
	@Test
	public void toStringTest() {
		
		Node.resetNodeCounter();
		
		PrefixTree tree = PrefixTree.getDummyPrefixTree();
		
		RootNode root = new RootNode(tree, PrefixTree.ROOT_NODE_ID);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) ]");

		@SuppressWarnings("unused")
		Node bot = new BotNode(null,root);
		
		root.addChild(PrefixTree.X);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] ]");
		
		Node three = root.addChild(8801);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] [id3 v8801 (null) ] ]");
		
		three.addChild(PrefixTree.X);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] [id3 v8801 (null) [id4 X (null) ] ] ]");
		
		Node five = root.addChild(8802);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] [id3 v8801 (null) [id4 X (null) ] ] [id5 v8802 (null) ] ]");
		
		five.addChild(PrefixTree.X);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] [id3 v8801 (null) [id4 X (null) ] ] [id5 v8802 (null) [id6 X (null) ] ] ]");
		
		root.addChild(8803);
		Assert.assertEquals(root.toString(), "[id1 ROOT (null) [id2 X (null) ] [id3 v8801 (null) [id4 X (null) ] ] [id5 v8802 (null) [id6 X (null) ] ] [id7 v8803 (null) ] ]");
				
	}
}
