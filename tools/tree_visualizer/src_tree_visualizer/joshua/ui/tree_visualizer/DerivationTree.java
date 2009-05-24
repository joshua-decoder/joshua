import edu.uci.ics.jung.graph.*;

import java.util.Scanner;
import java.util.LinkedList;
import java.util.Collection;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;

public class DerivationTree extends DirectedOrderedSparseMultigraph {
	// man are java regexes ugly
	// field seperator for joshua output
	public static final String DELIMITER = "\\|\\|\\|";
	public static final String SPACE = "\\s+";
	public static final int TGT_LINE = 795;
	public static final int SRC_LINE = 9;

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
			Object par = getPredecessors(v).toArray()[0];
			for (Node x : (Collection<Node>) getSuccessors(par)) {
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
