package joshua.ui.hypergraph_visualizer;

public class LeafVertex extends Vertex {
	private int english;
	private int targetPosition;
	
	public LeafVertex(int e, int pos)
	{
		english = e;
		targetPosition = pos;
	}
	
	public int getEnglish()
	{
		return english;
	}
	
	public int getTargetPosition()
	{
		return targetPosition;
	}
	
	public String toString()
	{
		return "LEAF";
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof LeafVertex))
			return false;
		LeafVertex other = (LeafVertex) o;
		return ((english == other.getEnglish())); // && (targetPosition == other.getTargetPosition()));
	}
}
