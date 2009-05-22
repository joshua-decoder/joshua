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

class Node {
	private String name;
	private int sourceStart;
	private int sourceEnd;
	private boolean isSource;

	public static final String DELIM = "[\\{\\}\\-]";

	public Node(String name, boolean isSource) {
		this.name = name;
		this.isSource = isSource;
	}

	public Node(String name, Node parent, boolean isSource)
	{
		this.name = name;
		this.isSource = isSource;
		String [] split;
		if (name.endsWith("}")) {
			split = name.split(DELIM);
		}
		else {
			split = parent.toString().split(DELIM);
		}
		sourceStart = Integer.parseInt(split[1]);
		sourceEnd = Integer.parseInt(split[2]);
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
}
