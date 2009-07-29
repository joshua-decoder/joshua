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
package joshua.ui.tree_visualizer;

import java.util.Scanner;
import java.util.LinkedList;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import joshua.util.Regex;

public class DerivationTree extends DirectedOrderedSparseMultigraph<Node,DerivationTreeEdge> {
	/**
	 * Eclipse thinks this is necessary.
	 */
	private static final long serialVersionUID = 2914449263979566324L;
	
	// man are java regexes ugly
	// field seperator for joshua output
	public static final String DELIMITER = "\\|\\|\\|";
	public static final String SPACE = "\\s+";
	public static final int TGT_LINE = 1;
	public static final int SRC_LINE = 1;

	private Node root;
	private Node sourceRoot;
	private String source;
	private LinkedList<Node> vertices;
	
	Node picked;

	public static void main(String [] argv)
	{
		try {
			int line = 1;
			Scanner tgt = new Scanner(new File(argv[0]), "UTF-8");
			DerivationTree g;
			while (line < TGT_LINE) {
				tgt.nextLine();
				line++;
			}
			if (argv.length > 1) {
				Scanner src = new Scanner(new File(argv[1]), "UTF-8");
				int srcLine = 1;
				while (srcLine < SRC_LINE) {
					src.nextLine();
					srcLine++;
				}
				g = new DerivationTree(tgt.nextLine().split(DELIMITER)[1], src.nextLine());
			}
			else {
				g = new DerivationTree(tgt.nextLine().split(DELIMITER)[1]);
			}


			JFrame frame = new JFrame("derivation tree");
			DerivationViewer viewer = new DerivationViewer(g, frame.getSize(), Color.red, DerivationViewer.AnchorType.ANCHOR_LEFTMOST_LEAF);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setSize(500, 500);
			frame.getContentPane().add(viewer);
			frame.pack();
			frame.setVisible(true);
		}
		catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}
		return;
	}

	public DerivationTree(String tree)
	{
		super();
		vertices = new LinkedList<Node>();
		graph(tree);
	}

	public DerivationTree(String tree, String source)
	{
		super();
		vertices = new LinkedList<Node>();
		this.source = source;
		graph(tree);
	}

	public Node getRoot()
	{
		return root;
	}

	public Node getSourceRoot()
	{
		return sourceRoot;
	}

	private void graph(String tree)
	{
		String [] toks = tree.replaceAll("\\)", "\n)").split(SPACE);
		treeToGraph(toks, null, 0);
		if (source != null) {
			addSourceNodes(root, null, 0);
		}
		return;
	}

	private int treeToGraph(String [] toks, Node parent, int curr)
	{
		String child = null;
		while (curr < toks.length) {
			String head = toks[curr];
			while (head.equals("")) {
				curr++;
				head = toks[curr];
			}
			curr++;
			if (head.equals(")")) {
				addVertexWithContext(child, parent, false);
				return curr;
			}
			if (head.startsWith("(")) {
				if (child != null) {
					addVertexWithContext(child, parent, false);
				}
				child = null;
				String nodeStr = head.substring(1);
				Node node = addVertexWithContext(nodeStr, parent, false);
				curr = treeToGraph(toks, node, curr);
			}
			else {
				if (child == null) {
					child = head;
				}
				else {
					child += " " + head;
				}
			}
		}
		return curr;
	}

	private Node addVertexWithContext(String child, Node parent, boolean isSource)
	{
		Node childNode;
		if (child == null)
			return null;
		if (parent != null) {
			if (source != null) {
				childNode = new Node(child, parent, isSource);
			}
			else {
				childNode = new Node(child, isSource);
			}
			addEdge(new DerivationTreeEdge(false), parent, childNode);
		}
		else {
			childNode = new Node(child, isSource);
			addVertex(childNode);
			if (isSource)
				sourceRoot = childNode;
			else
				root = childNode;
		}
		if (!isSource)
			vertices.add(childNode);
		return childNode;
	}

	private int addSourceNodes(Node curr, Node parent, int currentSourceIndex)
	{
		if (getSuccessors(curr).isEmpty())
			return currentSourceIndex;
		LinkedList<Node> children = new LinkedList<Node>(getSuccessors(curr));
		Node currSource = addVertexWithContext(curr.toString(), parent, true);
		currSource.setSourceSpan(curr.sourceStart(), curr.sourceEnd());
		while (!children.isEmpty()) {
			Node leftMost = children.get(0);
			for (Node n : children) {
				if (n.sourceStart() < leftMost.sourceStart())
					leftMost = n;
			}
			if (leftMost.sourceStart() > currentSourceIndex) {
				String [] sourceTokens = Regex.spaces.split(source);
				String sourceLeafName = sourceTokens[currentSourceIndex];
				for (int i = currentSourceIndex + 1; i < leftMost.sourceStart(); i++)
					sourceLeafName += " " + sourceTokens[i];
				addVertexWithContext(sourceLeafName, currSource, true);
				currentSourceIndex = leftMost.sourceStart();
			}
			else {
				currentSourceIndex = addSourceNodes(leftMost, currSource, leftMost.sourceStart());
				children.remove(leftMost);
			}
		}
		// HACK ALERT:
		// for some reason, the ROOT node in joshua output has a listed source span that is
		// one token longer than the actual source sentence. so we have to correct for that.
		int actualSourceEnd;
		if (parent == null) // ROOT node
			actualSourceEnd = currSource.sourceEnd() - 1;
		else
			actualSourceEnd = currSource.sourceEnd();
		if (actualSourceEnd > Regex.spaces.split(source).length)
			actualSourceEnd = Regex.spaces.split(source).length;
		if (currentSourceIndex < actualSourceEnd) {
			String [] sourceTokens = Regex.spaces.split(source);
			String sourceLeafName = sourceTokens[currentSourceIndex];
			for (int i = currentSourceIndex + 1; i < actualSourceEnd; i++)
				sourceLeafName += " " + sourceTokens[i];
			addVertexWithContext(sourceLeafName,currSource, true);
			currentSourceIndex = actualSourceEnd;
		}
		return currentSourceIndex;
	}

	public void addCorrespondences()
	{
		for (Node v : vertices) {
			Node s = v.getCounterpart();
			if (s != null)
				addEdge(new DerivationTreeEdge(true), v, s);
		}
	}
	
	public void setSubtreeHighlight(Node n, boolean b)
	{
		n.setHighlighted(b);
		for (Node s : getSuccessors(n)) {
			setSubtreeHighlight(s, b);
		}
		return;
	}
}
