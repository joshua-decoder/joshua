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

import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class DerivationTree extends DirectedOrderedSparseMultigraph<Node,DerivationTreeEdge> {
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
			DerivationViewer viewer = new DerivationViewer(g);

			JFrame frame = new JFrame("derivation tree");
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
			alignSource();
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
				addVertexWithContext(child, parent);
				return curr;
			}
			if (head.startsWith("(")) {
				if (child != null) {
					addVertexWithContext(child, parent);
				}
				child = null;
				String nodeStr = head.substring(1);
				Node node = addVertexWithContext(nodeStr, parent);
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

	private Node addVertexWithContext(String child, Node parent)
	{
		Node childNode;
		if (child == null)
			return null;
		if (parent != null) {
			if (source != null) {
				childNode = new Node(child, parent, false);
			}
			else {
				childNode = new Node(child, false);
			}
			addEdge(new DerivationTreeEdge(false), parent, childNode);
		}
		else {
			childNode = new Node(child, false);
			addVertex(childNode);
			root = childNode;
		}
		vertices.add(childNode);
		return childNode;
	}

	private void alignSource()
	{
	/*
		String [] toks = source.split("\\s+");
		Node [] src = new Node[toks.length];
		for (int i = 0; i < toks.length; i++) {
			src[i] = new Node(i + ". " + toks[i], true);
		}
	*/
		for (Node v : vertices) {
			if (outDegree(v) != 0)
				continue;
			int start = v.sourceStart();
			int end = v.sourceEnd();
			Node par = (Node) getPredecessors(v).toArray()[0];

			int v_index;
			Object [] siblings = getSuccessors(par).toArray();
			for (int i = 0; i < siblings.length; i++) {
				Node s = (Node) siblings[i];
				if (s == v) {
					if (i != 0)
						v.setSourceStart(((Node) siblings[i-1]).sourceEnd());
					else
						v.setSourceStart(par.sourceStart());
					if (i != siblings.length - 1)
						v.setSourceEnd(((Node) siblings[i+1]).sourceStart());
					else
						v.setSourceEnd(par.sourceEnd());
					break;
				}
			}
		/*
			for (Node x : getSuccessors(par)) {
				int xStart = x.sourceStart();
				int xEnd = x.sourceEnd();
				if (x == v) // pointer comparison
					continue;
				// we only deal with nonterminal siblings
				if (outDegree(x) == 0)
					continue;
				if ((xStart < end) && (xEnd >= end)) {
					end = xStart;
					v.setSourceSpan(start, end);
					continue;
				}
				if ((xEnd > start) && (xStart <= start)) {
					start = xEnd;
					v.setSourceSpan(start, end);
					continue;
				}
			}
		*/
			/*
			for (int j = start; j < end; j++) {
				addEdge(new DerivationTreeEdge(true), v, src[j]);
			}
			*/
		}
		addSourceNodes(root, null);
		return;
	}

	private void addSourceNodes(Node curr, Node parent)
	{
		LinkedList<Node> children = new LinkedList<Node>(getSuccessors(curr));
		if (children.isEmpty()) {
			String [] toks = source.split("\\s+");
			String srcName = "";
			for (int i = curr.sourceStart(); i < curr.sourceEnd(); i++) {
				srcName += toks[i] + " ";
			}
			if (srcName.equals(""))
				return;
			Node result = new Node(srcName, true);
			if (parent == null) {
				sourceRoot = result;
				addVertex(result);
			}
			else {
				addVertex(result);
				addEdge(new DerivationTreeEdge(false), parent, result);
			}
			curr.setCounterpart(result);
		}
		else {
			Node result = new Node(curr.toString(), true);
			result.setSourceSpan(curr.sourceStart(), curr.sourceEnd());
			addVertex(result);
			if (parent != null)
				addEdge(new DerivationTreeEdge(false), parent, result);
			else
				sourceRoot = result;
			while (!children.isEmpty()) {
				Node next = children.get(0);
				for (Node x : children) {
					if (x.sourceStart() < next.sourceStart())
						next = x;
				}
				addSourceNodes(next, result);
				children.remove(next);
			}
			/*
			int nextChild = curr.sourceStart();
			for (int i = 0; i < children.size(); i++) {
				Node x = children.get(i);
				if (x.sourceStart() == nextChild) {
					addSourceNodes(x, result);
					nextChild = x.sourceEnd();
					children.remove(i);
					i = -1;
				}
			}
			*/
		}
	}

	public void addCorrespondences()
	{
		for (Node v : vertices) {
			Node s = v.getCounterpart();
			if (s != null)
				addEdge(new DerivationTreeEdge(true), v, s);
		}
	}
}
