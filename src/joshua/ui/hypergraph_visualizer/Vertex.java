package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class Vertex {
	private boolean isHGNode;
	private HGNode node;
	private HyperEdge edge;

	public Vertex(HGNode n)
	{
		node = n;
		edge = null;
		isHGNode = true;
	}

	public Vertex(HyperEdge e)
	{
		edge = e;
		node = null;
		isHGNode = false;
	}

	public boolean isHGNode()
	{
		return isHGNode;
	}

	public HGNode node()
	{
		return node;
	}

	public HyperEdge edge()
	{
		return edge;
	}

	public String toString()
	{
		if (isHGNode) {
			return node.lhs + "{" + node.i + "-" + node.j + "}";
		}
		else {
			return edge.best_cost + ":" + edge.get_rule().toString();
		}
	}
}
