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

/**
 * A representation of a node in a derivation tree. The derivation tree class
 * itself is parameterized in terms of this class and the <code>DerivationEdge</code>
 * class. A <code>Node</code> may represent either a non-terminal symbol or one
 * or more terminal symbols of the derivation.
 */
public class Node {
	/**
	 * The label to be shown on the node. If the node is a non-terminal
	 * symbol, it is the name of the symbol. Otherwise, it is terminal
	 * symbols joined with spaces.
	 */
	private String name;

	/**
	 * Index into the source sentence of the first word that is aligned
	 * with this node.
	 */
	private int sourceStart;

	/**
	 * Index into the source sentence of the last word that is aligned
	 * with this node.
	 */
	private int sourceEnd;

	/**
	 * Indicates whether this node is part of the source-side of target-
	 * side derivation tree.
	 */
	private boolean isSource;

	/**
	 * If this node is a node holding terminal symbols, a pointer to the
	 * node that represents symbols that have been aligned with it.
	 */
	private Node counterpart = null;

	/**
	 * A regex used to extract source-side alignments from annotated
	 * non-terminals.
	 */
	public static final String DELIM = "[\\{\\}\\-]";
	
	/**
	 * A boolean to let the renderer know whether this vertex is highlighted.
	 */
	private boolean isHighlighted = false;

	/**
	 * Constructor used for root nodes or nodes whose parent is not given.
	 *
	 * @param name a <code>String</code> that represents the symbols at
	 *             this node
	 * @param isSource a boolean saying whether this is a source-side node
	 */
	public Node(String name, boolean isSource) {
		this.name = name;
		this.isSource = isSource;
		if (name.endsWith("}")) {
			this.name = name.substring(0, name.lastIndexOf("{"));
			String [] split = name.substring(name.lastIndexOf("{") + 1, name.length() - 1).split("\\-");
			sourceStart = Integer.parseInt(split[0]);
			sourceEnd = Integer.parseInt(split[1]);
		}
	}

	/**
	 * Constructor for nodes whose parent is known.
	 *
	 * @param name a <code>String</code> that represents the symbols at
	 *             this node
	 * @param parent the parent of this node
	 * @param isSource a boolean saying whether this is a source-side node
	 */
	public Node(String name, Node parent, boolean isSource)
	{
		this.name = name;
		this.isSource = isSource;
		if (name.endsWith("}")) {
			this.name = name.substring(0, name.lastIndexOf("{"));
			String [] split = name.substring(name.lastIndexOf("{") + 1, name.length() - 1).split("\\-");
			sourceStart = Integer.parseInt(split[0]);
			sourceEnd = Integer.parseInt(split[1]);
		}
		else {
			sourceStart = parent.sourceStart();
			sourceEnd = parent.sourceEnd();
		}
	}

	/**
	 * Returns a string representation of this node. That is, it returns
	 * the name of a non-terminal, or terminals that have been joined with
	 * spaces.
	 *
	 * @return a <code>String</code> representation of this node
	 */
	public String toString()
	{
		return name;
	}

	/**
	 * Sets the indices of the source sentence that are aligned with
	 * this node.
	 *
	 * @param start the index of the first source word that is aligned
	 * @param end the index after the last source word that is aligned
	 */
	public void setSourceSpan(int start, int end)
	{
		sourceStart = start;
		sourceEnd = end;
		return;
	}

	/**
	 * Sets the index of the first source word that is aligned with this
	 * node.
	 *
	 * @param x the value to set the index
	 */
	public void setSourceStart(int x)
	{
		sourceStart = x;
	}

	/**
	 * Sets the index after the last source word that is aligned with this
	 * node.
	 *
	 * @param x the value to set
	 */
	public void setSourceEnd(int x)
	{
		sourceEnd = x;
	}

	/**
	 * Returns whether this node is part of a source-side or target-side
	 * derivation tree.
	 *
	 * @return true if the node is part of a source-side tree, false
	 *         otherwise
	 */
	public boolean isSource()
	{
		return isSource;
	}

	/**
	 * Returns an index into the source sentence that is the first word
	 * aligned with this node.
	 *
	 * @return the index of the first source word aligned with this node
	 */
	public int sourceStart()
	{
		return sourceStart;
	}

	/**
	 * Returns an index into the source sentence that is one later than
	 * the last word that is aligned with this node.
	 *
	 * @return one plus the index of the last source word aligned with
	 *         this node
	 */
	public int sourceEnd()
	{
		return sourceEnd;
	}

	/**
	 * Returns the complete source phrase that is aligned with this node.
	 * We join together all the source words from start to end using
	 * one space per join.
	 *
	 * @param src the source sentence
	 *
	 * @return a substring of the source sentence that is aligned with
	 *         this node
	 */
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

	/**
	 * Sets this node's counterpart to the given node.
	 */
	public void setCounterpart(Node n)
	{
		counterpart = n;
	}

	/**
	 * Returns the node that represents that other-side node that has been
	 * aligned with this node.
	 *
	 * @return this node's counterpart
	 */
	public Node getCounterpart()
	{
		return counterpart;
	}
	
	public boolean isHighlighted()
	{
		return isHighlighted;
	}
	
	public void setHighlighted(boolean b)
	{
		isHighlighted = b;
	}
}
