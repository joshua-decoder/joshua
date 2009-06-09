package joshua.ui.hypergraph_visualizer;

public class Edge {
	private boolean isHighlighted;

	public Edge(boolean hl)
	{
		isHighlighted = hl;
	}

	public boolean isHighlighted()
	{
		return isHighlighted;
	}
}
