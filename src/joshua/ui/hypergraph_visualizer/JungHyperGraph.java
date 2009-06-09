package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.*;

import java.util.ArrayList;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;

public class JungHyperGraph extends DirectedOrderedSparseMultigraph<Vertex,Edge> {
	private Vertex root;

	public static void main(String [] argv)
	{
		System.out.println("hello, world");
		return;
	}

	public JungHyperGraph(HyperGraph hg)
	{
		root = new Vertex(hg.goal_item);
		addNode(hg.goal_item, null);
	}

	public Vertex getRoot()
	{
		return root;
	}

	private void addNode(HGNode n, Vertex edge_out)
	{
		Vertex v = new Vertex(n);
		if (getVertices().contains(v)) {
			addEdge(new Edge(false), v, edge_out);
			return;
		}
		if (edge_out != null)
			addEdge(new Edge(false), v, edge_out);
		for (HyperEdge e : n.l_hyperedges)
			addHyperEdge(v, e);
		return;
	}

	private void addHyperEdge(Vertex parent, HyperEdge e)
	{
		if (e == null)
			return;
		Vertex v = new Vertex(e);
		addEdge(new Edge(false), v, parent);
		for (HGNode n : e.get_ant_items())
			addNode(n, v);
		return;
	}
}
