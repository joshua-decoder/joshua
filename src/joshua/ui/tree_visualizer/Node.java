/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.ui.tree_visualizer;

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

	public void setSourceStart(int x)
	{
		sourceStart = x;
	}

	public void setSourceEnd(int x)
	{
		sourceEnd = x;
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
