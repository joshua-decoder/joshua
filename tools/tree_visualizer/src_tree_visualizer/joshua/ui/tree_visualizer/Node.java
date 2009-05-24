// a wrapper class
// so that we can have nodes with the same labels without them
// being the same node.
// JUNG uses equals() comparison rather than pointer comparison to see
// if two nodes are the same.
class Node {
	private String name;
	private int sourceStart;
	private int sourceEnd;
	private boolean isSource;
	private Node counterpart = null;

	public static final String DELIM = "[\\{\\}\\-]";

	public Node(String name, boolean isSource) {
		this.name = name;
		this.isSource = isSource;
		if (name.endsWith("}")) {
			String [] split = name.split(DELIM);
			this.name = split[0];
			sourceStart = Integer.parseInt(split[1]);
			sourceEnd = Integer.parseInt(split[2]);
		}
	}

	public Node(String name, Node parent, boolean isSource)
	{
		this.name = name;
		this.isSource = isSource;
		if (name.endsWith("}")) {
			String [] split = name.split(DELIM);
			this.name = split[0];
			sourceStart = Integer.parseInt(split[1]);
			sourceEnd = Integer.parseInt(split[2]);
		}
		else {
			sourceStart = parent.sourceStart();
			sourceEnd = parent.sourceEnd();
		}
	}

	public String toString()
	{
		return name;
	}

	public void setSourceSpan(int start, int end)
	{
		sourceStart = start;
		sourceEnd = end;
		return;
	}

	public boolean isSource()
	{
		return isSource;
	}

	public int sourceStart()
	{
		return sourceStart;
	}

	public int sourceEnd()
	{
		return sourceEnd;
	}

	public String source(String src)
	{
		int i;
		String [] toks = src.split("\\s+");
		String ret = toks[sourceStart];
		for (i = sourceStart + 1; i < sourceEnd; i++) {
			ret += " " + toks[i];
		}
		return ret;
	}

	public void setCounterpart(Node n)
	{
		counterpart = n;
	}

	public Node getCounterpart()
	{
		return counterpart;
	}
}
