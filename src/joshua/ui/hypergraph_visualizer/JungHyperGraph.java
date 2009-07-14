package joshua.ui.hypergraph_visualizer;

import java.util.HashMap;

import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.hypergraph.*;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;

public class JungHyperGraph extends DirectedOrderedSparseMultigraph<Vertex,Edge> {
	private Vertex root;
	
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
		DiskHyperGraph dhg = new DiskHyperGraph(new Vocabulary(), 0, true, null);
		dhg.initRead(itemsFile, rulesFile, chosenSentences);
		JungHyperGraph hg = new JungHyperGraph(dhg.readHyperGraph());
		return;
	}

	public JungHyperGraph(HyperGraph hg)
	{
		root = new Vertex(hg.goal_item, 0, 0);
	//	addNode(hg.goal_item, null, 0, 0);
	}

	public Vertex getRoot()
	{
		return root;
	}

	void addNode(HGNode n, Vertex edge_out, int height, int width)
	{
		Vertex v = new Vertex(n, height, width);
		if (getVertices().contains(v)) {
			addEdge(new Edge(false), v, edge_out);
			return;
		}
		if (edge_out != null)
			addEdge(new Edge(false), v, edge_out);
		int i = 0;
		try {
		Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			System.err.println("sleep was interrupted!");
		}
		for (HyperEdge e : n.l_hyperedges) {
			addHyperEdge(v, e, height + 1, i);
			i++;
		}
		return;
	}

	private void addHyperEdge(Vertex parent, HyperEdge e, int height, int width)
	{
		if (e == null)
			return;
		Vertex v = new Vertex(e, height, width);
		addEdge(new Edge(false), v, parent);
		if (e.get_ant_items() == null)
			return;
		int i = 0;
		for (HGNode n : e.get_ant_items()) {
			addNode(n, v, height + 1, i);
			i++;
		}
		return;
	}
}
