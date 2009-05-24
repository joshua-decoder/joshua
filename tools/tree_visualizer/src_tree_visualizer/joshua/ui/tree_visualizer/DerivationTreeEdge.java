public class DerivationTreeEdge {
	private boolean pointsToSource;

	public DerivationTreeEdge(boolean pts)
	{
		pointsToSource = pts;
	}

	public boolean pointsToSource()
	{
		return pointsToSource;
	}
}
