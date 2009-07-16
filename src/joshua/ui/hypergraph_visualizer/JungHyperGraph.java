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
		vocab = st;
		targetIndex = 0;
		addNode(hg.goal_item, null);
	}

	public Vertex getRoot()
	{
		return root;
	}

	private void addNode(HGNode n, Vertex edge_out)
	{
		Vertex v = new NodeVertex(n);
		if (getVertices().contains(v)) {
			addEdge(new Edge(false), edge_out, v);
			return;
		}
		if (edge_out != null)
			addEdge(new Edge(false), edge_out, v);
		else
			root = v;
		addHyperEdge(v, n.best_hyperedge);
		return;
	}

	private void addHyperEdge(Vertex parent, HyperEdge e)
	{
		if (e == null)
			return;
		Vertex v = new HyperEdgeVertex(e);
		addEdge(new Edge(false), parent, v);
		if (e.get_rule() != null) {
			ArrayList<HGNode> items = null;
			if (e.get_ant_items() != null)
				items = new ArrayList<HGNode>(e.get_ant_items());
			for (int t : e.get_rule().getEnglish()) {
				if (vocab.isNonterminal(t)) {
					addNode(items.get(0), v);
					items.remove(0);
				}
				else {
					addEdge(new Edge(false), v, new LeafVertex(t, targetIndex));
					targetIndex++;
				}
			}
		}
		else {
			for (HGNode i : e.get_ant_items()) {
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
}
