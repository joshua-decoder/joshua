package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.HyperEdge;

public class HyperEdgeVertex extends Vertex {
	private HyperEdge edge;
	
	public HyperEdgeVertex(HyperEdge e)
	{
		edge = e;
	}
	
	public HyperEdge getHyperEdge()
	{
		return edge;
	}
	
	public String toString()
	{
		return "EDGE";
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof HyperEdgeVertex))
			return false;
		HyperEdgeVertex other = (HyperEdgeVertex) o;
		return (edge.equals(other.getHyperEdge()));
	}
}
