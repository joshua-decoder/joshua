package joshua.ui.hypergraph_visualizer;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public abstract class Vertex {
	private int color = 0;
	
	public Vertex() {
		return;
	}
	
	public int getColor()
	{
		return color;
	}
	
	public void setColor(int c)
	{
		color = c;
		return;
	}
}
