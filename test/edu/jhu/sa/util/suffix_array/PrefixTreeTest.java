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
package edu.jhu.sa.util.suffix_array;

import java.util.HashMap;
import java.util.Map;


import org.testng.Assert;
import org.testng.annotations.Test;

import edu.jhu.sa.util.suffix_array.PrefixTree;
import edu.jhu.sa.util.suffix_array.PrefixTree.Node;


/**
 *
 * 
 * @author Lane Schwartz
 */
public class PrefixTreeTest {

	int it = 0;
	int persuades = 1;
	int him = 2;
	int and = 3;
	int disheartens = 4;
	
	int[] sentence = {it, persuades, him, and, it, disheartens, him};
	
	int maxPhraseSpan = 5;
	int maxPhraseLength = 5;
	int maxNonterminals = 2;
	int spanLimit = 8;
	
	
	
	PrefixTree tree;
	
	@Test(dependsOnMethods = {"prefixTreeNodes","suffixLinks"})
	public void setup() {
		
		Map<Integer,String> idToString = new HashMap<Integer,String>();
		idToString.put(it, "it");
		idToString.put(persuades, "persuades");
		idToString.put(him, "him");
		idToString.put(and, "and");
		idToString.put(disheartens, "disheartens");
		idToString.put(PrefixTree.X, "X");
		idToString.put(PrefixTree.ROOT_NODE_ID, "ROOT");
		idToString.put(PrefixTree.BOT_NODE_ID, "BOT");
		
		PrefixTree.idsToStrings = idToString;
		
		tree = new PrefixTree(sentence, maxPhraseSpan, maxPhraseLength, maxNonterminals, spanLimit);
		
		//System.err.println(tree.toString());		
		//System.err.println(tree.size());
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void size() {
		Assert.assertEquals(tree.size(), 134);
	}
	
	@Test(dependsOnMethods = {"prefixTreeNodes"})
	public void suffixLinks() {
		
		PrefixTree tree = PrefixTree.getDummyPrefixTree();
		
		Node bot = tree.new Node(-999);
		Node root = tree.new Node(-1);
		bot.children = PrefixTree.botMap(root);
		root.linkToSuffix(bot);
		
		Assert.assertEquals(root.suffixLink, bot);
		
		Node root_x = root.addChild(PrefixTree.X);
		Node root_x_him = root_x.addChild(him);
		Node root_x_him_x = root_x_him.addChild(PrefixTree.X);
		
		Node root_him = root.addChild(him);
		Node root_him_x = root_him.addChild(PrefixTree.X);
		
		Node root_him_suffixLink = PrefixTree.calculateSuffixLink(root, him);
		Assert.assertEquals(root_him_suffixLink, root);
		root_him.linkToSuffix(root_him_suffixLink);
		Assert.assertEquals(root_him.suffixLink, root_him_suffixLink);
		
		Node root_x_suffixLink = PrefixTree.calculateSuffixLink(root, PrefixTree.X);
		Assert.assertEquals(root_x_suffixLink, root);
		root_x.linkToSuffix(root_x_suffixLink);
		Assert.assertEquals(root_x.suffixLink, root_x_suffixLink);
		
		Node root_x_him_suffixLink = PrefixTree.calculateSuffixLink(root_x, him);
		Assert.assertEquals(root_x_him_suffixLink, root_him);
		root_x_him.linkToSuffix(root_x_him_suffixLink);
		Assert.assertEquals(root_x_him.suffixLink, root_x_him_suffixLink);
		
		Node root_x_him_x_suffixLink = PrefixTree.calculateSuffixLink(root_x_him, PrefixTree.X);
		Assert.assertEquals(root_x_him_x_suffixLink, root_him_x);
		root_x_him_x.linkToSuffix(root_x_him_x_suffixLink);
		Assert.assertEquals(root_x_him_x.suffixLink, root_x_him_x_suffixLink);

		PrefixTree.resetNodeCounter();
	}	
	
	
	Node root_X, root_it, root_persuades, root_him, root_and, root_disheartens;
	
	@Test(dependsOnMethods = {"setup"})
	public void rootChildren() {

		// The root node should have 6 children
		Assert.assertEquals(tree.root.children.size(), 6); // 5 terminals plus X
		
		Assert.assertTrue(tree.root.hasChild(it));
		Assert.assertTrue(tree.root.hasChild(persuades));
		Assert.assertTrue(tree.root.hasChild(him));
		Assert.assertTrue(tree.root.hasChild(and));
		Assert.assertTrue(tree.root.hasChild(disheartens));
		Assert.assertTrue(tree.root.hasChild(PrefixTree.X));
		
		root_it = tree.root.getChild(it);
		root_persuades = tree.root.getChild(persuades);
		root_him = tree.root.getChild(him);
		root_and = tree.root.getChild(and);
		root_disheartens = tree.root.getChild(disheartens);
		root_X = tree.root.getChild(PrefixTree.X);
		
	}
	
		
	Node root_it_persuades, root_persuades_him, root_him_and, root_and_it, root_it_disheartens, root_disheartens_him;
	Node root_it_X, root_persuades_X, root_him_X, root_and_X, root_disheartens_X;
	
	@Test(dependsOnMethods = {"rootChildren"})
	public void rootOtherGrandchildren() {

		Assert.assertEquals(root_it.children.size(), 3);
		Assert.assertTrue(root_it.hasChild(persuades));
		Assert.assertTrue(root_it.hasChild(disheartens));
		Assert.assertTrue(root_it.hasChild(PrefixTree.X));
		root_it_persuades = root_it.getChild(persuades);
		root_it_disheartens = root_it.getChild(disheartens);
		root_it_X = root_it.getChild(PrefixTree.X);
		
		Assert.assertEquals(root_persuades.children.size(), 2);
		Assert.assertTrue(root_persuades.hasChild(him));
		Assert.assertTrue(root_persuades.hasChild(PrefixTree.X));
		root_persuades_him = root_persuades.getChild(him);
		root_persuades_X = root_persuades.getChild(PrefixTree.X);
		
		Assert.assertEquals(root_him.children.size(), 2);
		Assert.assertTrue(root_him.hasChild(and));
		Assert.assertTrue(root_him.hasChild(PrefixTree.X));	
		root_him_and = root_him.getChild(and);
		root_him_X = root_him.getChild(PrefixTree.X);
		
		Assert.assertEquals(root_and.children.size(), 2);
		Assert.assertTrue(root_and.hasChild(it));
		Assert.assertTrue(root_and.hasChild(PrefixTree.X));	
		root_and_it = root_and.getChild(it);
		root_and_X = root_and.getChild(PrefixTree.X);
		
		Assert.assertEquals(root_disheartens.children.size(), 2);
		Assert.assertTrue(root_disheartens.hasChild(him));
		Assert.assertTrue(root_disheartens.hasChild(PrefixTree.X));		
		root_disheartens_him = root_disheartens.getChild(him);
		root_disheartens_X = root_disheartens.getChild(PrefixTree.X);
		
	}
	
	
	Node root_it_persuades_him, root_it_persuades_X;
	Node root_it_disheartens_him, root_it_disheartens_X;
	Node root_it_X_him, root_it_X_and, root_it_X_it;
	
	Node root_persuades_him_and, root_persuades_him_X;
	Node root_persuades_X_and, root_persuades_X_it, root_persuades_X_disheartens;
	
	Node root_him_and_it, root_him_and_X;
	Node root_him_X_it, root_him_X_disheartens, root_him_X_him;
	
	Node root_and_it_disheartens, root_and_it_X;
	Node root_and_X_disheartens, root_and_X_him;
	
	
	@Test(dependsOnMethods = {"rootOtherGrandchildren"})
	public void rootOtherGreatGrandchildren() {

		// Node root_it_persuades_him, root_it_persuades_X;
		Assert.assertEquals(root_it_persuades.children.size(), 2);
		Assert.assertTrue(root_it_persuades.hasChild(him));
		Assert.assertTrue(root_it_persuades.hasChild(PrefixTree.X));
		root_it_persuades_him = root_it_persuades.getChild(him);
		root_it_persuades_X = root_it_persuades.getChild(PrefixTree.X);
		
		// Node root_it_disheartens_him, root_it_disheartens_X;
		Assert.assertEquals(root_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_it_disheartens.hasChild(him));
		Assert.assertTrue(root_it_disheartens.hasChild(PrefixTree.X));
		root_it_disheartens_him = root_it_disheartens.getChild(him);
		root_it_disheartens_X = root_it_disheartens.getChild(PrefixTree.X);
		
		// Node root_it_X_him, root_it_X_and, root_it_X_it;
		Assert.assertEquals(root_it_X.children.size(), 3);
		Assert.assertTrue(root_it_X.hasChild(him));
		Assert.assertTrue(root_it_X.hasChild(and));
		Assert.assertTrue(root_it_X.hasChild(it));
		root_it_X_him = root_it_X.getChild(him);
		root_it_X_and = root_it_X.getChild(and);
		root_it_X_it = root_it_X.getChild(it);
		
		
		// Node root_persuades_him_and, root_persuades_him_X;
		Assert.assertEquals(root_persuades_him.children.size(), 2);
		Assert.assertTrue(root_persuades_him.hasChild(and));
		Assert.assertTrue(root_persuades_him.hasChild(PrefixTree.X));
		root_persuades_him_and = root_persuades_him.getChild(and);
		root_persuades_him_X = root_persuades_him.getChild(PrefixTree.X);
		
		// Node root_persuades_X_and, root_persuades_X_it, root_persuades_X_disheartens;
		Assert.assertEquals(root_persuades_X.children.size(), 3);
		Assert.assertTrue(root_persuades_X.hasChild(and));
		Assert.assertTrue(root_persuades_X.hasChild(it));
		Assert.assertTrue(root_persuades_X.hasChild(disheartens));
		root_persuades_X_and = root_persuades_X.getChild(and);
		root_persuades_X_it = root_persuades_X.getChild(it);
		root_persuades_X_disheartens = root_persuades_X.getChild(disheartens);
		
		
		// Node root_him_and_it, root_him_and_X;
		Assert.assertEquals(root_him_and.children.size(), 2);
		Assert.assertTrue(root_him_and.hasChild(it));
		Assert.assertTrue(root_him_and.hasChild(PrefixTree.X));
		root_him_and_it = root_him_and.getChild(it);
		root_him_and_X = root_him_and.getChild(PrefixTree.X);
		
		
		// Node root_him_X_it, root_him_X_disheartens, root_him_X_him;
		Assert.assertEquals(root_him_X.children.size(), 3);
		Assert.assertTrue(root_him_X.hasChild(it));
		Assert.assertTrue(root_him_X.hasChild(disheartens));
		Assert.assertTrue(root_him_X.hasChild(him));
		root_him_X_it = root_him_X.getChild(it);
		root_him_X_disheartens = root_him_X.getChild(disheartens);
		root_him_X_him = root_him_X.getChild(him);
		
		
		// Node root_and_it_disheartens, root_and_it_X;
		Assert.assertEquals(root_and_it.children.size(), 2);
		Assert.assertTrue(root_and_it.hasChild(disheartens));
		Assert.assertTrue(root_and_it.hasChild(PrefixTree.X));
		root_and_it_disheartens = root_and_it.getChild(disheartens);
		root_and_it_X = root_and_it.getChild(PrefixTree.X);
		
		
		// Node root_and_X_disheartens, root_and_X_him;
		Assert.assertEquals(root_and_X.children.size(), 2);
		Assert.assertTrue(root_and_X.hasChild(disheartens));
		Assert.assertTrue(root_and_X.hasChild(him));
		root_and_X_disheartens = root_and_X.getChild(disheartens);
		root_and_X_him = root_and_X.getChild(him);
		
	}
	
	Node root_it_persuades_him_and, root_it_persuades_him_X;
	Node root_it_persuades_X_and, root_it_persuades_X_it;
	
	Node root_it_X_him_and, root_it_X_him_X;
	Node root_it_X_and_it, root_it_X_and_X;
	
	Node root_persuades_him_and_it, root_persuades_him_and_X;
	Node root_persuades_him_X_it, root_persuades_him_X_disheartens;
	
	Node root_persuades_X_and_it, root_persuades_X_and_X;
	Node root_persuades_X_it_disheartens, root_persuades_X_it_X;
	
	Node root_him_and_it_disheartens, root_him_and_it_X;
	Node root_him_and_X_disheartens, root_him_and_X_him;
	
	Node root_him_X_it_disheartens, root_him_X_it_X;
	Node root_him_X_disheartens_him;
	
	Node root_and_it_disheartens_him;
	Node root_and_it_X_him;
	Node root_and_X_disheartens_him;
	
	Node root_him_X_disheartens_X, root_and_it_disheartens_X, root_and_X_disheartens_X;
	
	@Test(dependsOnMethods = {"rootOtherGreatGrandchildren"})
	public void rootOtherGreatGreatGrandchildren() {
		
		// Node root_it_persuades_him_and, root_it_persuades_him_X;
		Assert.assertEquals(root_it_persuades_him.children.size(), 2);
		Assert.assertTrue(root_it_persuades_him.hasChild(and));
		Assert.assertTrue(root_it_persuades_him.hasChild(PrefixTree.X));
		root_it_persuades_him_and = root_it_persuades_him.getChild(and);
		root_it_persuades_him_X = root_it_persuades_him.getChild(PrefixTree.X);
		
		// Node root_it_persuades_X_and, root_it_persuades_X_it;
		Assert.assertEquals(root_it_persuades_X.children.size(), 2);
		Assert.assertTrue(root_it_persuades_X.hasChild(and));
		Assert.assertTrue(root_it_persuades_X.hasChild(it));
		root_it_persuades_X_and = root_it_persuades_X.getChild(and);
		root_it_persuades_X_it = root_it_persuades_X.getChild(it);
		
		// Node root_it_disheartens_him_and, root_it_disheartens_him_X;
		Assert.assertEquals(root_it_disheartens_him.children.size(), 0);
		
		
		// Node root_it_disheartens_X_and, root_it_disheartens_X_it;
		Assert.assertEquals(root_it_disheartens_X.children.size(), 0);

		
		// Node root_it_X_him_and, root_it_X_him_X;
		Assert.assertEquals(root_it_X_him.children.size(), 2);
		Assert.assertTrue(root_it_X_him.hasChild(and));
		Assert.assertTrue(root_it_X_him.hasChild(PrefixTree.X));
		root_it_X_him_and = root_it_X_him.getChild(and);
		root_it_X_him_X = root_it_X_him.getChild(PrefixTree.X);
		
		// Node root_it_X_and_it, root_it_X_and_X;
		Assert.assertEquals(root_it_X_and.children.size(), 2);
		Assert.assertTrue(root_it_X_and.hasChild(it));
		Assert.assertTrue(root_it_X_and.hasChild(PrefixTree.X));
		root_it_X_and_it = root_it_X_and.getChild(it);
		root_it_X_and_X = root_it_X_and.getChild(PrefixTree.X);
		
		// Node root_persuades_him_and_it, root_persuades_him_and_X;
		Assert.assertEquals(root_persuades_him_and.children.size(), 2);
		Assert.assertTrue(root_persuades_him_and.hasChild(it));
		Assert.assertTrue(root_persuades_him_and.hasChild(PrefixTree.X));
		root_persuades_him_and_it = root_persuades_him_and.getChild(it);
		root_persuades_him_and_X = root_persuades_him_and.getChild(PrefixTree.X);
		
		// Node root_persuades_him_X_it, root_persuades_him_X_disheartens;
		Assert.assertEquals(root_persuades_him_X.children.size(), 2);
		Assert.assertTrue(root_persuades_him_X.hasChild(it));
		Assert.assertTrue(root_persuades_him_X.hasChild(disheartens));
		root_persuades_him_X_it = root_persuades_him_X.getChild(it);
		root_persuades_him_X_disheartens = root_persuades_him_X.getChild(disheartens);
		
		// Node root_persuades_X_and_it, root_persuades_X_and_X;
		Assert.assertEquals(root_persuades_X_and.children.size(), 2);
		Assert.assertTrue(root_persuades_X_and.hasChild(it));
		Assert.assertTrue(root_persuades_X_and.hasChild(PrefixTree.X));
		root_persuades_X_and_it = root_persuades_X_and.getChild(it);
		root_persuades_X_and_X = root_persuades_X_and.getChild(PrefixTree.X);
		
		
		// Node root_persuades_X_it_disheartens, root_persuades_X_it_X;
		Assert.assertEquals(root_persuades_X_it.children.size(), 2);
		Assert.assertTrue(root_persuades_X_it.hasChild(disheartens));
		Assert.assertTrue(root_persuades_X_it.hasChild(PrefixTree.X));
		root_persuades_X_it_disheartens = root_persuades_X_it.getChild(disheartens);
		root_persuades_X_it_X = root_persuades_X_it.getChild(PrefixTree.X);
		
		// Node root_him_and_it_disheartens, root_him_and_it_X;
		Assert.assertEquals(root_him_and_it.children.size(), 2);
		Assert.assertTrue(root_him_and_it.hasChild(disheartens));
		Assert.assertTrue(root_him_and_it.hasChild(PrefixTree.X));
		root_him_and_it_disheartens = root_him_and_it.getChild(disheartens);
		root_him_and_it_X = root_him_and_it.getChild(PrefixTree.X);
		
		// Node root_him_and_X_disheartens, root_him_and_X_him;
		Assert.assertEquals(root_him_and_X.children.size(), 2);
		Assert.assertTrue(root_him_and_X.hasChild(disheartens));
		Assert.assertTrue(root_him_and_X.hasChild(him));
		root_him_and_X_disheartens = root_him_and_X.getChild(disheartens);
		root_him_and_X_him = root_him_and_X.getChild(him);
		
		// Node root_him_X_it_disheartens, root_him_X_it_X;
		Assert.assertEquals(root_him_X_it.children.size(), 2);
		Assert.assertTrue(root_him_X_it.hasChild(disheartens));
		Assert.assertTrue(root_him_X_it.hasChild(PrefixTree.X));
		root_him_X_it_disheartens = root_him_X_it.getChild(disheartens);
		root_him_X_it_X = root_him_X_it.getChild(PrefixTree.X);
		
		// Node root_him_X_disheartens_him, root_him_X_disheartens_X;
		Assert.assertEquals(root_him_X_disheartens.children.size(), 2);
		Assert.assertTrue(root_him_X_disheartens.hasChild(him));
		Assert.assertTrue(root_him_X_disheartens.hasChild(PrefixTree.X));
		root_him_X_disheartens_him = root_him_X_disheartens.getChild(him);
		root_him_X_disheartens_X = root_him_X_disheartens.getChild(PrefixTree.X);
		
		
		// Node root_and_it_disheartens_him, root_and_it_disheartens_X;
		Assert.assertEquals(root_and_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_and_it_disheartens.hasChild(him));
		Assert.assertTrue(root_and_it_disheartens.hasChild(PrefixTree.X));
		root_and_it_disheartens_him = root_and_it_disheartens.getChild(him);
		root_and_it_disheartens_X = root_and_it_disheartens.getChild(PrefixTree.X);
		
		// Node root_and_it_X_him;
		Assert.assertEquals(root_and_it_X.children.size(), 1);
		Assert.assertTrue(root_and_it_X.hasChild(him));
		root_and_it_X_him = root_and_it_X.getChild(him);
		
		// Node root_and_X_disheartens_him, root_and_X_disheartens_X;
		Assert.assertEquals(root_and_X_disheartens.children.size(), 2);
		Assert.assertTrue(root_and_X_disheartens.hasChild(him));
		Assert.assertTrue(root_and_X_disheartens.hasChild(PrefixTree.X));
		root_and_X_disheartens_him = root_and_X_disheartens.getChild(him);
		root_and_X_disheartens_X = root_and_X_disheartens.getChild(PrefixTree.X);
		
	}
	
	
	Node root_it_persuades_him_and_it, root_it_persuades_him_and_X;
	Node root_it_persuades_him_X_it;
	Node root_it_persuades_X_and_it, root_it_persuades_X_and_X;
	
	Node root_it_X_him_and_it, root_it_X_him_and_X;
	Node root_it_X_him_X_it;
	
	Node root_persuades_him_and_it_disheartens, root_persuades_him_and_it_X;
	Node root_persuades_him_and_X_disheartens;
	Node root_persuades_him_X_it_disheartens, root_persuades_him_X_it_X;
	
	Node root_persuades_X_and_it_disheartens, root_persuades_X_and_it_X;
	Node root_persuades_X_and_X_disheartens;
	
	Node root_him_and_it_disheartens_him, root_him_and_it_disheartens_X;
	Node root_him_and_it_X_him;
	Node root_him_and_X_disheartens_him, root_him_and_X_disheartens_X;
	
	Node root_him_X_it_disheartens_him, root_him_X_it_disheartens_X;
	Node root_him_X_it_X_him;
	
	
	@Test(dependsOnMethods = {"rootOtherGreatGreatGrandchildren"})
	public void rootOtherGreatGreatGreatGrandchildren() {
		
		// Node root_it_persuades_him_and_it, root_it_persuades_him_and_X;
		Assert.assertEquals(root_it_persuades_him_and.children.size(), 2);
		Assert.assertTrue(root_it_persuades_him_and.hasChild(it));
		Assert.assertTrue(root_it_persuades_him_and.hasChild(PrefixTree.X));
		root_it_persuades_him_and_it = root_it_persuades_him_and.getChild(it);
		root_it_persuades_him_and_X = root_it_persuades_him_and.getChild(PrefixTree.X);
		Assert.assertEquals(root_it_persuades_him_and_it.children.size(), 0);
		Assert.assertEquals(root_it_persuades_him_and_X.children.size(), 0);
		
		// Node root_it_persuades_him_X_and, root_it_persuades_him_X_it;
		Assert.assertEquals(root_it_persuades_him_X.children.size(), 1);
		Assert.assertTrue(root_it_persuades_him_X.hasChild(it));
		root_it_persuades_him_X_it = root_it_persuades_him_X.getChild(it);
		Assert.assertEquals(root_it_persuades_him_X_it.children.size(), 0);
		
		// Node root_it_persuades_X_and_it, root_it_persuades_X_and_X;
		Assert.assertEquals(root_it_persuades_X_and.children.size(), 2);
		Assert.assertTrue(root_it_persuades_X_and.hasChild(it));
		Assert.assertTrue(root_it_persuades_X_and.hasChild(PrefixTree.X));
		root_it_persuades_X_and_it = root_it_persuades_X_and.getChild(it);
		root_it_persuades_X_and_X = root_it_persuades_X_and.getChild(PrefixTree.X);
		Assert.assertEquals(root_it_persuades_X_and_it.children.size(), 0);
		Assert.assertEquals(root_it_persuades_X_and_X.children.size(), 0);
		
		
		// Node root_it_X_him_and_it, root_it_X_him_and_X;
		Assert.assertEquals(root_it_X_him_and.children.size(), 2);
		Assert.assertTrue(root_it_X_him_and.hasChild(it));
		Assert.assertTrue(root_it_X_him_and.hasChild(PrefixTree.X));
		root_it_X_him_and_it = root_it_X_him_and.getChild(it);
		root_it_X_him_and_X = root_it_X_him_and.getChild(PrefixTree.X);
		Assert.assertEquals(root_it_X_him_and_it.children.size(), 0);
		Assert.assertEquals(root_it_X_him_and_X.children.size(), 0);
		
		
		// Node root_it_X_him_X_it;
		Assert.assertEquals(root_it_X_him_X.children.size(), 1);
		Assert.assertTrue(root_it_X_him_X.hasChild(it));
		root_it_X_him_X_it = root_it_X_him_X.getChild(it);
		Assert.assertEquals(root_it_X_him_X_it.children.size(), 0);
		
		// Node root_persuades_him_and_it_disheartens, root_persuades_him_and_it_X;
		Assert.assertEquals(root_persuades_him_and_it.children.size(), 2);
		Assert.assertTrue(root_persuades_him_and_it.hasChild(disheartens));
		Assert.assertTrue(root_persuades_him_and_it.hasChild(PrefixTree.X));
		root_persuades_him_and_it_disheartens = root_persuades_him_and_it.getChild(disheartens);
		root_persuades_him_and_it_X = root_persuades_him_and_it.getChild(PrefixTree.X);
		Assert.assertEquals(root_persuades_him_and_it_disheartens.children.size(), 0);
		Assert.assertEquals(root_persuades_him_and_it_X.children.size(), 0);
		
		// Node root_persuades_him_and_X_disheartens;
		Assert.assertEquals(root_persuades_him_and_X.children.size(), 1);
		Assert.assertTrue(root_persuades_him_and_X.hasChild(disheartens));
		root_persuades_him_and_X_disheartens = root_persuades_him_and_X.getChild(disheartens);
		Assert.assertEquals(root_persuades_him_and_X_disheartens.children.size(), 0);
		
		// Node root_persuades_him_X_it_disheartens, root_persuades_him_X_it_X;
		Assert.assertEquals(root_persuades_him_X_it.children.size(), 2);
		Assert.assertTrue(root_persuades_him_X_it.hasChild(disheartens));
		Assert.assertTrue(root_persuades_him_X_it.hasChild(PrefixTree.X));
		root_persuades_him_X_it_disheartens = root_persuades_him_X_it.getChild(disheartens);
		root_persuades_him_X_it_X = root_persuades_him_X_it.getChild(PrefixTree.X);
		Assert.assertEquals(root_persuades_him_X_it_disheartens.children.size(), 0);
		Assert.assertEquals(root_persuades_him_X_it_X.children.size(), 0);
		
		// Node root_persuades_X_and_it_disheartens, root_persuades_X_and_it_X;
		Assert.assertEquals(root_persuades_X_and_it.children.size(), 2);
		Assert.assertTrue(root_persuades_X_and_it.hasChild(disheartens));
		Assert.assertTrue(root_persuades_X_and_it.hasChild(PrefixTree.X));
		root_persuades_X_and_it_disheartens = root_persuades_X_and_it.getChild(disheartens);
		root_persuades_X_and_it_X = root_persuades_X_and_it.getChild(PrefixTree.X);
		Assert.assertEquals(root_persuades_X_and_it_disheartens.children.size(), 0);
		Assert.assertEquals(root_persuades_X_and_it_X.children.size(), 0);
		
		// Node root_persuades_X_and_X_disheartens;
		Assert.assertEquals(root_persuades_X_and_X.children.size(), 1);
		Assert.assertTrue(root_persuades_X_and_X.hasChild(disheartens));
		root_persuades_X_and_X_disheartens = root_persuades_X_and_X.getChild(disheartens);
		Assert.assertEquals(root_persuades_X_and_X_disheartens.children.size(), 0);
		
		// Node root_him_and_it_disheartens_him, root_him_and_it_disheartens_X;
		Assert.assertEquals(root_him_and_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_him_and_it_disheartens.hasChild(him));
		Assert.assertTrue(root_him_and_it_disheartens.hasChild(PrefixTree.X));
		root_him_and_it_disheartens_him = root_him_and_it_disheartens.getChild(him);
		root_him_and_it_disheartens_X = root_him_and_it_disheartens.getChild(PrefixTree.X);
		Assert.assertEquals(root_him_and_it_disheartens_him.children.size(), 0);
		Assert.assertEquals(root_him_and_it_disheartens_X.children.size(), 0);
		
		// Node root_him_and_it_X_him;
		Assert.assertEquals(root_him_and_it_X.children.size(), 1);
		Assert.assertTrue(root_him_and_it_X.hasChild(him));
		root_him_and_it_X_him = root_him_and_it_X.getChild(him);
		Assert.assertEquals(root_him_and_it_X_him.children.size(), 0);
		
		// Node root_him_and_X_disheartens_him, root_him_and_X_disheartens_X;
		Assert.assertEquals(root_him_and_X_disheartens.children.size(), 2);
		Assert.assertTrue(root_him_and_X_disheartens.hasChild(him));
		Assert.assertTrue(root_him_and_X_disheartens.hasChild(PrefixTree.X));
		root_him_and_X_disheartens_him = root_him_and_X_disheartens.getChild(him);
		root_him_and_X_disheartens_X = root_him_and_X_disheartens.getChild(PrefixTree.X);
		Assert.assertEquals(root_him_and_X_disheartens_him.children.size(), 0);
		Assert.assertEquals(root_him_and_X_disheartens_X.children.size(), 0);
		
		// Node root_him_X_it_disheartens_him, root_him_X_it_disheartens_X;
		Assert.assertEquals(root_him_X_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_him_X_it_disheartens.hasChild(him));
		Assert.assertTrue(root_him_X_it_disheartens.hasChild(PrefixTree.X));
		root_him_X_it_disheartens_him = root_him_X_it_disheartens.getChild(him);
		root_him_X_it_disheartens_X = root_him_X_it_disheartens.getChild(PrefixTree.X);
		Assert.assertEquals(root_him_X_it_disheartens_him.children.size(), 0);
		Assert.assertEquals(root_him_X_it_disheartens_X.children.size(), 0);
		
		// Node root_him_X_it_X_him;
		Assert.assertEquals(root_him_X_it_X.children.size(), 1);
		Assert.assertTrue(root_him_X_it_X.hasChild(him));
		root_him_X_it_X_him = root_him_X_it_X.getChild(him);
		Assert.assertEquals(root_him_X_it_X_him.children.size(), 0);
		
	}
	
	@Test(dependsOnMethods = {"rootChildren"})
	public void rootChildrenSuffixLinks() {

		// The suffixLink of each child of root should point back to root
		Assert.assertEquals(root_it.suffixLink, tree.root);
		Assert.assertEquals(root_persuades.suffixLink, tree.root);
		Assert.assertEquals(root_him.suffixLink, tree.root);
		Assert.assertEquals(root_and.suffixLink, tree.root);
		Assert.assertEquals(root_disheartens.suffixLink, tree.root);
		Assert.assertEquals(root_X.suffixLink, tree.root);
		
	}
	
	@Test(dependsOnMethods = {"rootOtherGrandchildren"})
	public void rootOtherGrandchildrenSuffixLinks() {
		
		Assert.assertEquals(root_him.getChild(PrefixTree.X).suffixLink, root_X);
		
		Assert.assertEquals(root_it_persuades.suffixLink, root_persuades);
		Assert.assertEquals(root_it_disheartens.suffixLink, root_disheartens);
		Assert.assertEquals(root_it_X.suffixLink, root_X);
		
		Assert.assertEquals(root_persuades_him.suffixLink, root_him);
		Assert.assertEquals(root_persuades_X.suffixLink, root_X);
		
		Assert.assertEquals(root_him_and.suffixLink, root_and);
		Assert.assertEquals(root_him_X.suffixLink, root_X);
		
		Assert.assertEquals(root_and_it.suffixLink, root_it);
		Assert.assertEquals(root_and_X.suffixLink, root_X);
			
		Assert.assertEquals(root_disheartens_him.suffixLink, root_him);
		Assert.assertEquals(root_disheartens_X.suffixLink, root_X);
		
	}

	@Test(dependsOnMethods = {"rootOtherGreatGrandchildren"})
	public void rootOtherGreatGrandchildrenSuffixLinks() {

		Assert.assertEquals(root_it_persuades_him.suffixLink, root_persuades_him);
		Assert.assertEquals(root_it_persuades_X.suffixLink, root_persuades_X);

		Assert.assertEquals(root_it_disheartens_him.suffixLink, root_disheartens_him);
		Assert.assertEquals(root_it_disheartens_X.suffixLink, root_disheartens_X);

		Assert.assertEquals(root_it_X_him.suffixLink, root_X_him);
		Assert.assertEquals(root_it_X_and.suffixLink, root_X_and);
		Assert.assertEquals(root_it_X_it.suffixLink, root_X_it);

		Assert.assertEquals(root_persuades_him_and.suffixLink, root_him_and);
		Assert.assertEquals(root_persuades_him_X.suffixLink, root_him_X);

		Assert.assertEquals(root_persuades_X_and.suffixLink, root_X_and);
		Assert.assertEquals(root_persuades_X_it.suffixLink, root_X_it);
		Assert.assertEquals(root_persuades_X_disheartens.suffixLink, root_X_disheartens);

		Assert.assertEquals(root_him_and_it.suffixLink, root_and_it);
		Assert.assertEquals(root_him_and_X.suffixLink, root_and_X);

		Assert.assertEquals(root_him_X_it.suffixLink, root_X_it);
		Assert.assertEquals(root_him_X_disheartens.suffixLink, root_X_disheartens);
		Assert.assertEquals(root_him_X_him.suffixLink, root_X_him);

		Assert.assertEquals(root_and_it_disheartens.suffixLink, root_it_disheartens);
		Assert.assertEquals(root_and_it_X.suffixLink, root_it_X);

		Assert.assertEquals(root_and_X_disheartens.suffixLink, root_X_disheartens);
		Assert.assertEquals(root_and_X_him.suffixLink, root_X_him);

	}
	
	@Test(dependsOnMethods = {"rootOtherGreatGreatGrandchildren"})
	public void rootOtherGreatGreatGrandchildrenSuffixLinks() {
		
		Assert.assertEquals(root_it_persuades_him_and.suffixLink, root_persuades_him_and);
		Assert.assertEquals(root_it_persuades_him_X.suffixLink, root_persuades_him_X);
		
		Assert.assertEquals(root_it_persuades_X_and.suffixLink, root_persuades_X_and);
		Assert.assertEquals(root_it_persuades_X_it.suffixLink, root_persuades_X_it);
		
		Assert.assertEquals(root_it_X_him_and.suffixLink, root_X_him_and);
		Assert.assertEquals(root_it_X_him_X.suffixLink, root_X_him_X);
		
		Assert.assertEquals(root_it_X_and_it.suffixLink, root_X_and_it);
		Assert.assertEquals(root_it_X_and_X.suffixLink, root_X_and_X);
		
		Assert.assertEquals(root_persuades_him_and_it.suffixLink, root_him_and_it);
		Assert.assertEquals(root_persuades_him_and_X.suffixLink, root_him_and_X);
		
		Assert.assertEquals(root_persuades_him_X_it.suffixLink, root_him_X_it);
		Assert.assertEquals(root_persuades_him_X_disheartens.suffixLink, root_him_X_disheartens);
		
		Assert.assertEquals(root_persuades_X_and_it.suffixLink, root_X_and_it);
		Assert.assertEquals(root_persuades_X_and_X.suffixLink, root_X_and_X);
		
		Assert.assertEquals(root_persuades_X_it_disheartens.suffixLink, root_X_it_disheartens);
		Assert.assertEquals(root_persuades_X_it_X.suffixLink, root_X_it_X);
		
		Assert.assertEquals(root_him_and_it_disheartens.suffixLink, root_and_it_disheartens);
		Assert.assertEquals(root_him_and_it_X.suffixLink, root_and_it_X);
		
		Assert.assertEquals(root_him_and_X_disheartens.suffixLink, root_and_X_disheartens);
		Assert.assertEquals(root_him_and_X_him.suffixLink, root_and_X_him);
		
		Assert.assertEquals(root_him_X_it_disheartens.suffixLink, root_X_it_disheartens);
		Assert.assertEquals(root_him_X_it_X.suffixLink, root_X_it_X);
		
		Assert.assertEquals(root_him_X_disheartens_him.suffixLink, root_X_disheartens_him);
		Assert.assertEquals(root_him_X_disheartens_X.suffixLink, root_X_disheartens_X);
		
		Assert.assertEquals(root_and_it_disheartens_him.suffixLink, root_it_disheartens_him);
		Assert.assertEquals(root_and_it_disheartens_X.suffixLink, root_it_disheartens_X);
		
		Assert.assertEquals(root_and_it_X_him.suffixLink, root_it_X_him);
		
		Assert.assertEquals(root_and_X_disheartens_him.suffixLink, root_X_disheartens_him);
		Assert.assertEquals(root_and_X_disheartens_X.suffixLink, root_X_disheartens_X);
		
	}
	
	@Test(dependsOnMethods = {"rootOtherGreatGreatGreatGrandchildren"})
	public void rootOtherGreatGreatGreatGrandchildrenSuffixLinks() {
		
		Assert.assertEquals(root_it_persuades_him_and_it.suffixLink, root_persuades_him_and_it);
		Assert.assertEquals(root_it_persuades_him_and_X.suffixLink, root_persuades_him_and_X);
		
		Assert.assertEquals(root_it_persuades_him_X_it.suffixLink, root_persuades_him_X_it);
		
		Assert.assertEquals(root_it_persuades_X_and_it.suffixLink, root_persuades_X_and_it);
		Assert.assertEquals(root_it_persuades_X_and_X.suffixLink, root_persuades_X_and_X);
		
		Assert.assertEquals(root_it_X_him_and_it.suffixLink, root_X_him_and_it);
		Assert.assertEquals(root_it_X_him_and_X.suffixLink, root_X_him_and_X);
		
		Assert.assertEquals(root_it_X_him_X_it.suffixLink, root_X_him_X_it);
		
		Assert.assertEquals(root_persuades_him_and_it_disheartens.suffixLink, root_him_and_it_disheartens);
		Assert.assertEquals(root_persuades_him_and_it_X.suffixLink, root_him_and_it_X);
		
		Assert.assertEquals(root_persuades_him_and_X_disheartens.suffixLink, root_him_and_X_disheartens);
		
		Assert.assertEquals(root_persuades_him_X_it_disheartens.suffixLink, root_him_X_it_disheartens);
		Assert.assertEquals(root_persuades_him_X_it_X.suffixLink, root_him_X_it_X);
		
		Assert.assertEquals(root_persuades_X_and_it_disheartens.suffixLink, root_X_and_it_disheartens);
		Assert.assertEquals(root_persuades_X_and_it_X.suffixLink, root_X_and_it_X);
		
		Assert.assertEquals(root_persuades_X_and_X_disheartens.suffixLink, root_X_and_X_disheartens);
		
		Assert.assertEquals(root_him_and_it_disheartens_him.suffixLink, root_and_it_disheartens_him);
		Assert.assertEquals(root_him_and_it_disheartens_X.suffixLink, root_and_it_disheartens_X);
		
		Assert.assertEquals(root_him_and_it_X_him.suffixLink, root_and_it_X_him);
		
		Assert.assertEquals(root_him_and_X_disheartens_him.suffixLink, root_and_X_disheartens_him);
		Assert.assertEquals(root_him_and_X_disheartens_X.suffixLink, root_and_X_disheartens_X);
	
		Assert.assertEquals(root_him_X_it_disheartens_him.suffixLink, root_X_it_disheartens_him);
		Assert.assertEquals(root_him_X_it_disheartens_X.suffixLink, root_X_it_disheartens_X);
		
		Assert.assertEquals(root_him_X_it_X_him.suffixLink, root_X_it_X_him);
		
	}

	
	
	Node root_X_persuades, root_X_him, root_X_and, root_X_it, root_X_disheartens;

	@Test(dependsOnMethods = {"rootChildren"})
	public void rootXChildren() {
		
		Assert.assertTrue(root_X.hasChild(persuades));
		Assert.assertTrue(root_X.hasChild(him));
		Assert.assertTrue(root_X.hasChild(and));
		Assert.assertTrue(root_X.hasChild(it));
		Assert.assertTrue(root_X.hasChild(disheartens));
		
		Assert.assertFalse(root_X.hasChild(PrefixTree.X));

		Assert.assertEquals(root_X.children.size(), 5);
		
		root_X_persuades = root_X.getChild(persuades);
		root_X_him = root_X.getChild(him);
		root_X_and = root_X.getChild(and);
		root_X_it = root_X.getChild(it);
		root_X_disheartens = root_X.getChild(disheartens);

	}
	

	Node root_X_persuades_him, root_X_him_and, root_X_and_it, root_X_it_disheartens, root_X_disheartens_him;
	Node root_X_persuades_X, root_X_him_X, root_X_and_X, root_X_it_X, root_X_disheartens_X;
	
	@Test(dependsOnMethods = {"rootXChildren"})
	public void rootXGrandchildren() {
		
		Assert.assertEquals(root_X_persuades.children.size(), 2);
		Assert.assertTrue(root_X_persuades.hasChild(him));
		Assert.assertTrue(root_X_persuades.hasChild(PrefixTree.X));
		root_X_persuades_him = root_X_persuades.getChild(him);
		root_X_persuades_X = root_X_persuades.getChild(PrefixTree.X);		
		
		Assert.assertEquals(root_X_him.children.size(), 2);
		Assert.assertTrue(root_X_him.hasChild(and));
		Assert.assertTrue(root_X_him.hasChild(PrefixTree.X));
		root_X_him_and = root_X_him.getChild(and);
		root_X_him_X = root_X_him.getChild(PrefixTree.X);	
		
		Assert.assertEquals(root_X_and.children.size(), 2);
		Assert.assertTrue(root_X_and.hasChild(it));
		Assert.assertTrue(root_X_and.hasChild(PrefixTree.X));
		root_X_and_it = root_X_and.getChild(it);
		root_X_and_X = root_X_and.getChild(PrefixTree.X);

		Assert.assertEquals(root_X_it.children.size(), 2);
		Assert.assertTrue(root_X_it.hasChild(disheartens));
		Assert.assertTrue(root_X_it.hasChild(PrefixTree.X));
		root_X_it_disheartens = root_X_it.getChild(disheartens);
		root_X_it_X = root_X_it.getChild(PrefixTree.X);
		
		Assert.assertEquals(root_X_disheartens.children.size(), 2);
		Assert.assertTrue(root_X_disheartens.hasChild(him));
		Assert.assertTrue(root_X_disheartens.hasChild(PrefixTree.X));
		root_X_disheartens_him = root_X_disheartens.getChild(him);
		root_X_disheartens_X = root_X_disheartens.getChild(PrefixTree.X);
	
	}
	
	Node root_X_persuades_him_and, root_X_him_and_it, root_X_and_it_disheartens, root_X_it_disheartens_him;
	Node root_X_persuades_him_X, root_X_him_and_X, root_X_and_it_X, root_X_it_disheartens_X;
	Node root_X_persuades_X_and, root_X_him_X_it, root_X_and_X_disheartens, root_X_it_X_him;
	Node root_X_persuades_X_it, root_X_him_X_disheartens, root_X_and_X_him;
	
	@Test(dependsOnMethods = {"rootXGrandchildren"})
	public void rootXGreatGrandchildren() {
		
		Assert.assertEquals(root_X_persuades_him.children.size(), 2);
		Assert.assertTrue(root_X_persuades_him.hasChild(and));
		Assert.assertTrue(root_X_persuades_him.hasChild(PrefixTree.X));
		root_X_persuades_him_and = root_X_persuades_him.getChild(and);
		root_X_persuades_him_X = root_X_persuades_him.getChild(PrefixTree.X);		
		
		Assert.assertEquals(root_X_him_and.children.size(), 2);
		Assert.assertTrue(root_X_him_and.hasChild(it));
		Assert.assertTrue(root_X_him_and.hasChild(PrefixTree.X));
		root_X_him_and_it = root_X_him_and.getChild(it);
		root_X_him_and_X = root_X_him_and.getChild(PrefixTree.X);	
		
		Assert.assertEquals(root_X_and_it.children.size(), 2);
		Assert.assertTrue(root_X_and_it.hasChild(disheartens));
		Assert.assertTrue(root_X_and_it.hasChild(PrefixTree.X));
		root_X_and_it_disheartens = root_X_and_it.getChild(disheartens);
		root_X_and_it_X = root_X_and_it.getChild(PrefixTree.X);

		Assert.assertEquals(root_X_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_X_it_disheartens.hasChild(him));
		Assert.assertTrue(root_X_it_disheartens.hasChild(PrefixTree.X));
		root_X_it_disheartens_him = root_X_it_disheartens.getChild(him);
		root_X_it_disheartens_X = root_X_it_disheartens.getChild(PrefixTree.X);
		
		
		///////
		
		
		Assert.assertEquals(root_X_persuades_X.children.size(), 2);
		Assert.assertTrue(root_X_persuades_X.hasChild(and));
		Assert.assertTrue(root_X_persuades_X.hasChild(it));
		root_X_persuades_X_and = root_X_persuades_X.getChild(and);
		root_X_persuades_X_it = root_X_persuades_X.getChild(it);		
		
		Assert.assertEquals(root_X_him_X.children.size(), 2);
		Assert.assertTrue(root_X_him_X.hasChild(it));
		Assert.assertTrue(root_X_him_X.hasChild(disheartens));
		root_X_him_X_it = root_X_him_X.getChild(it);
		root_X_him_X_disheartens = root_X_him_X.getChild(disheartens);	
		
		Assert.assertEquals(root_X_and_X.children.size(), 2);
		Assert.assertTrue(root_X_and_X.hasChild(disheartens));
		Assert.assertTrue(root_X_and_X.hasChild(him));
		root_X_and_X_disheartens = root_X_and_X.getChild(disheartens);
		root_X_and_X_him = root_X_and_X.getChild(him);

		Assert.assertEquals(root_X_it_X.children.size(), 1);
		Assert.assertTrue(root_X_it_X.hasChild(him));
		root_X_it_X_him = root_X_it_X.getChild(him);
		
		Assert.assertEquals(root_X_disheartens.children.size(), 2);
		Assert.assertTrue(root_X_disheartens.hasChild(him));
		Assert.assertTrue(root_X_disheartens.hasChild(PrefixTree.X));
		root_X_disheartens_him = root_X_disheartens.getChild(him);
		root_X_disheartens_X = root_X_disheartens.getChild(PrefixTree.X);
	
	}
	

	Node root_X_persuades_him_and_it, root_X_persuades_him_and_X;
	Node root_X_him_and_it_disheartens, root_X_him_and_it_X;
	Node root_X_and_it_disheartens_him, root_X_and_it_disheartens_X;
	
	Node root_X_persuades_him_X_it, root_X_him_and_X_disheartens, root_X_and_it_X_him;
	
	Node root_X_persuades_X_and_it, root_X_him_X_it_disheartens, root_X_and_X_disheartens_him;
	
	@Test(dependsOnMethods = {"rootXGreatGrandchildren"})
	public void rootXGreatGreatGrandchildren() {
		
		Assert.assertEquals(root_X_persuades_him_and.children.size(), 2);
		Assert.assertTrue(root_X_persuades_him_and.hasChild(it));
		Assert.assertTrue(root_X_persuades_him_and.hasChild(PrefixTree.X));
		root_X_persuades_him_and_it = root_X_persuades_him_and.getChild(it);
		root_X_persuades_him_and_X = root_X_persuades_him_and.getChild(PrefixTree.X);		
		Assert.assertEquals(root_X_persuades_him_and_it.children.size(), 0);
		Assert.assertEquals(root_X_persuades_him_and_X.children.size(), 0);
		
		Assert.assertEquals(root_X_him_and_it.children.size(), 2);
		Assert.assertTrue(root_X_him_and_it.hasChild(disheartens));
		Assert.assertTrue(root_X_him_and_it.hasChild(PrefixTree.X));
		root_X_him_and_it_disheartens = root_X_him_and_it.getChild(disheartens);
		root_X_him_and_it_X = root_X_him_and_it.getChild(PrefixTree.X);	
		Assert.assertEquals(root_X_him_and_it_disheartens.children.size(), 0);
		Assert.assertEquals(root_X_him_and_it_X.children.size(), 0);
		
		Assert.assertEquals(root_X_and_it_disheartens.children.size(), 2);
		Assert.assertTrue(root_X_and_it_disheartens.hasChild(him));
		Assert.assertTrue(root_X_and_it_disheartens.hasChild(PrefixTree.X));
		root_X_and_it_disheartens_him = root_X_and_it_disheartens.getChild(him);
		root_X_and_it_disheartens_X = root_X_and_it_disheartens.getChild(PrefixTree.X);
		Assert.assertEquals(root_X_and_it_disheartens_him.children.size(), 0);
		Assert.assertEquals(root_X_and_it_disheartens_X.children.size(), 0);
		
		///////
		
		Assert.assertEquals(root_X_persuades_him_X.children.size(), 1);
		Assert.assertTrue(root_X_persuades_him_X.hasChild(it));
		root_X_persuades_him_X_it = root_X_persuades_him_X.getChild(it);
		Assert.assertEquals(root_X_persuades_him_X_it.children.size(), 0);
		
		Assert.assertEquals(root_X_him_and_X.children.size(), 1);
		Assert.assertTrue(root_X_him_and_X.hasChild(disheartens));
		root_X_him_and_X_disheartens = root_X_him_and_X.getChild(disheartens);
		Assert.assertEquals(root_X_him_and_X_disheartens.children.size(), 0);
		
		Assert.assertEquals(root_X_and_it_X.children.size(), 1);
		Assert.assertTrue(root_X_and_it_X.hasChild(him));
		root_X_and_it_X_him = root_X_and_it_X.getChild(him);
		Assert.assertEquals(root_X_and_it_X_him.children.size(), 0);
		
		///////
		
		//Node root_X_persuades_X_and_it, root_X_him_X_it_disheartens, root_X_and_X_disheartens_him;
		
		Assert.assertEquals(root_X_persuades_X_and.children.size(), 1);
		Assert.assertTrue(root_X_persuades_X_and.hasChild(it));
		root_X_persuades_X_and_it = root_X_persuades_X_and.getChild(it);
		Assert.assertEquals(root_X_persuades_X_and_it.children.size(), 0);
		
		Assert.assertEquals(root_X_him_X_it.children.size(), 1);
		Assert.assertTrue(root_X_him_X_it.hasChild(disheartens));
		root_X_him_X_it_disheartens = root_X_him_X_it.getChild(disheartens);
		Assert.assertEquals(root_X_him_X_it_disheartens.children.size(), 0);
		
		Assert.assertEquals(root_X_and_X_disheartens.children.size(), 1);
		Assert.assertTrue(root_X_and_X_disheartens.hasChild(him));
		root_X_and_X_disheartens_him = root_X_and_X_disheartens.getChild(him);
		Assert.assertEquals(root_X_and_X_disheartens_him.children.size(), 0);
		
		/////
		
		//Node root_X_persuades_X_it_disheartens, root_X_him_X_disheartens_him;
		Assert.assertEquals(root_X_persuades_X_it.children.size(), 0);
		Assert.assertEquals(root_X_him_X_disheartens.children.size(), 0);
		
	}
	
	
	
	@Test(dependsOnMethods = {"rootXChildren"})
	public void rootXChildrenSuffixLinks() {
		
		Assert.assertEquals(root_X_persuades.suffixLink, root_persuades);
		Assert.assertEquals(root_X_him.suffixLink, root_him);
		Assert.assertEquals(root_X_and.suffixLink, root_and);
		Assert.assertEquals(root_X_it.suffixLink, root_it);
		Assert.assertEquals(root_X_disheartens.suffixLink, root_disheartens);
		
	}
	
	
	
	
	
	
	
	@Test(dependsOnMethods = {"rootXGrandchildren","rootOtherGrandchildren"})
	public void rootXGrandchildrenSuffixLinks() {
		
		Assert.assertEquals(root_X_him.getChild(PrefixTree.X).suffixLink,root_him.getChild(PrefixTree.X));
		
		Assert.assertEquals(root_X_persuades_him.suffixLink, root_persuades_him);
		Assert.assertEquals(root_X_persuades_X.suffixLink, root_persuades_X);		
		
		Assert.assertEquals(root_X_him_and.suffixLink, root_him_and);
		Assert.assertEquals(root_X_him_X.suffixLink, root_him_X);
		
		Assert.assertEquals(root_X_and_it.suffixLink, root_and_it);
		Assert.assertEquals(root_X_and_X.suffixLink, root_and_X);

		Assert.assertEquals(root_X_it_disheartens.suffixLink, root_it_disheartens);
		Assert.assertEquals(root_X_it_X.suffixLink, root_it_X);
		
		Assert.assertEquals(root_X_disheartens_him.suffixLink, root_disheartens_him);
		Assert.assertEquals(root_X_disheartens_X.suffixLink, root_disheartens_X);
	}
	
	@Test(dependsOnMethods = {"rootXGrandchildren","rootOtherGreatGrandchildren"})
	public void rootXGreatGrandchildrenSuffixLinks() {
		
		Assert.assertEquals(root_X_persuades_him_and.suffixLink, root_persuades_him_and);
		Assert.assertEquals(root_X_persuades_him_X.suffixLink, root_persuades_him_X);		
		
		Assert.assertEquals(root_X_him_and_it.suffixLink, root_him_and_it);
		Assert.assertEquals(root_X_him_and_X.suffixLink, root_him_and_X);	
		
		Assert.assertEquals(root_X_and_it_disheartens.suffixLink, root_and_it_disheartens);
		Assert.assertEquals(root_X_and_it_X.suffixLink, root_and_it_X);

		Assert.assertEquals(root_X_it_disheartens_him.suffixLink, root_it_disheartens_him);
		Assert.assertEquals(root_X_it_disheartens_X.suffixLink, root_it_disheartens_X);
		
		
		///////
		
		Assert.assertEquals(root_X_persuades_X_and.suffixLink, root_persuades_X_and);
		Assert.assertEquals(root_X_persuades_X_it.suffixLink, root_persuades_X_it);		

		Assert.assertEquals(root_X_him_X_it.suffixLink, root_him_X_it);
		Assert.assertEquals(root_X_him_X_disheartens.suffixLink, root_him_X_disheartens);	
		
		Assert.assertEquals(root_X_and_X_disheartens.suffixLink, root_and_X_disheartens);
		Assert.assertEquals(root_X_and_X_him.suffixLink, root_and_X_him);

		Assert.assertEquals(root_X_it_X_him.suffixLink, root_it_X_him);		
		
		Assert.assertEquals(root_X_disheartens_him.suffixLink, root_disheartens_him);
		Assert.assertEquals(root_X_disheartens_X.suffixLink, root_disheartens_X);
		
	
	}
	
	

	
	@Test
	public void prefixTreeNodes() {
		
		PrefixTree tree = PrefixTree.getDummyPrefixTree();
		
		Node node = tree.new Node(-999);
		
		Assert.assertEquals(node.active, Node.ACTIVE);
		Assert.assertNull(node.suffixLink);
		Assert.assertTrue(node.children.isEmpty());
		
		int child = -1;
		
		node.addChild(child);
		
		Assert.assertTrue(node.hasChild(child));
		
		Assert.assertFalse(node.children.isEmpty());
		Assert.assertEquals(node.children.size(), 1);
		
		Assert.assertNotNull(node.getChild(child));
		
		PrefixTree.resetNodeCounter();
		
	}
}
