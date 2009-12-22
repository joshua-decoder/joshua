package joshua.ui.hypergraph_visualizer;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.hypergraph.*;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;

public class JungHyperGraph extends DirectedOrderedSparseMultigraph<Vertex,Edge> {
	private Vertex root;
	private SymbolTable vocab;
	private int targetIndex;
	
	private boolean checkDuplicateVertices;
	
	public Vertex picked;
	
	private ArrayList<Vertex> nonTreeEdgeEndpoints;
	
	public static final String USAGE = "usage: JungHyperGraph <items file> <rules file> <first sentence> <last sentence>";

	public static void main(String [] argv)
	{
		if (argv.length < 4) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String itemsFile = argv[0];
		String rulesFile = argv[1];
		int firstSentence = Integer.parseInt(argv[2]);
		int lastSentence = Integer.parseInt(argv[3]);
		HashMap<Integer,Integer> chosenSentences = new HashMap<Integer,Integer>();
		for (int i = firstSentence; i < lastSentence; i++) {
			chosenSentences.put(i, i);
		}
		Vocabulary vocab = new Vocabulary();
		DiskHyperGraph dhg = new DiskHyperGraph(vocab, 0, true, null);
		dhg.initRead(itemsFile, rulesFile, chosenSentences);
		JungHyperGraph hg = new JungHyperGraph(dhg.readHyperGraph(), vocab);
		return;
	}

	public JungHyperGraph(HyperGraph hg, SymbolTable st)
	{
		checkDuplicateVertices = false;
		vocab = st;
		targetIndex = 0;
		nonTreeEdgeEndpoints = new ArrayList<Vertex>();
		addNode(hg.goalNode, null);
	}

	public Vertex getRoot()
	{
		return root;
	}

	public void addNode(HGNode n, Vertex edge_out)
	{
		Vertex v = new NodeVertex(n);
		if (checkDuplicateVertices && getVertices().contains(v)) {
//			addEdge(new Edge(false), edge_out, v);
			ArrayList<Vertex> vertexList = (ArrayList<Vertex>) getVertices();
			nonTreeEdgeEndpoints.add(edge_out);
			nonTreeEdgeEndpoints.add(vertexList.get(vertexList.indexOf(v)));
			return;
		}
		if (edge_out != null)
			addEdge(new Edge(false), edge_out, v);
		else
			root = v;
		addHyperEdge(v, n.bestHyperedge);
		return;
	}

	public void addHyperEdge(Vertex parent, HyperEdge e)
	{
		if (e == null)
			return;
		Vertex v = new HyperEdgeVertex(e);
		addEdge(new Edge(false), parent, v);
		if (e.getRule() != null) {
			ArrayList<HGNode> items = null;
			if (e.getAntNodes() != null)
				items = new ArrayList<HGNode>(e.getAntNodes());
			for (int t : e.getRule().getEnglish()) {
				if (vocab.isNonterminal(t)) {
					addNode(items.get(0), v);
					items.remove(0);
				}
				else {
					LeafVertex leaf = new LeafVertex(t, targetIndex);
					if (checkDuplicateVertices && getVertices().contains(leaf)) {	
						System.err.println("Leaf already in tree: " + t + "/" + targetIndex);
						ArrayList<Vertex> vertexList = (ArrayList<Vertex>) getVertices();
						nonTreeEdgeEndpoints.add(v);
						nonTreeEdgeEndpoints.add(vertexList.get(vertexList.indexOf(leaf)));
					}
					else {
						addEdge(new Edge(false), v, leaf);
					}
					targetIndex++;
				}
			}
		}
		else {
			for (HGNode i : e.getAntNodes()) {
				addNode(i, v);
			}
		}
		return;
	}
	
	public void incrementHyperEdge(NodeVertex v)
	{
		removeSubtreeBelow(v);
		addHyperEdge(v, v.incrementEdge());
		return;
	}
	
	private void removeSubtreeBelow(Vertex v)
	{
		if (getSuccessorCount(v) == 0)
			return;
		else {
			for (Vertex s : getSuccessors(v)) {
				removeSubtreeBelow(s);
				for (Edge e : findEdgeSet(v, s)) {
					removeEdge(e);
				}
				removeVertex(s);
			}
		}
		return;
	}
	
	public void addNonTreeEdges()
	{
		int i = 0;
		while (i < nonTreeEdgeEndpoints.size() - 2) {
			addEdge(new Edge(false), nonTreeEdgeEndpoints.get(i), nonTreeEdgeEndpoints.get(i + 1));
			i += 2;
		}
		return;
	}
	
	public void replaceSubtrees(Object [] objectList)
	{
		if (objectList.length < 1)
			return;
		nonTreeEdgeEndpoints.clear();
		removeSubtreeBelow(picked);
		checkDuplicateVertices = true;
		for (Object o : objectList) {
			HyperEdge e = (HyperEdge) o;
			addHyperEdge(picked, e);
		}
		int color = 1;
		for (Vertex v : getSuccessors(picked)) {
			setSubTreeColor(v, color);
			color++;
		}
		checkDuplicateVertices = false;
		return;
	}
	
	public void setSubTreeColor(Vertex v, int color)
	{
		v.setColor(color);
		for (Vertex s : getSuccessors(v)) {
			setSubTreeColor(s, color);
		}
	}
}
