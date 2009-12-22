package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class NodeVertex extends Vertex {
	private HGNode node;
	private int edgeIndex;
	
	public NodeVertex(HGNode n)
	{
		node = n;
		edgeIndex = 0;
	}
	
	public HGNode getNode()
	{
		return node;
	}
	
	public String toString()
	{
		return "NODE";
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof NodeVertex))
			return false;
		NodeVertex other = (NodeVertex) o;
		return (node.equals(other.getNode()));
	}
	
	public HyperEdge incrementEdge()
	{
		if (edgeIndex == node.hyperedges.size() - 1)
			edgeIndex = 0;
		else
			edgeIndex++;
		return node.hyperedges.get(edgeIndex);
	}
}
