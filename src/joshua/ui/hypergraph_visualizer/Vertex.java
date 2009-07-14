package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class Vertex {
	private boolean isHGNode;
	private HGNode node;
	private HyperEdge edge;
	private int height;
	private int width;

	public Vertex(HGNode n, int h, int w)
	{
		node = n;
		edge = null;
		height = h;
		width = w;
		isHGNode = true;
	}

	public Vertex(HyperEdge e, int h, int w)
	{
		edge = e;
		node = null;
		height = h;
		width = w;
		isHGNode = false;
	}
	
	public int height()
	{
		return height;
	}
	
	public int width()
	{
		return width;
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
			return HyperGraphViewer.vocab.getWord(node.lhs) + "{" + node.i + "-" + node.j + "}";
		}
		else {
			return edge.best_cost + ":" + edge.get_rule();
		}
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof Vertex))
			return false;
		Vertex otherVertex = (Vertex) o;
		if (node != null)
			return (node == otherVertex.node());
		if (edge != null)
			return (edge == otherVertex.edge());
		return false;
	}
}
