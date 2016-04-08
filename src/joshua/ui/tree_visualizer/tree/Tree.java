/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.ui.tree_visualizer.tree;

import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * A class to represent the target-side tree produced by decoding using Joshua
 * with an SCFG.
 * <p>
 * When decoding with use_tree_nbest=true, instead of a flat text output like
 * "i asked her a question", we get a Penn treebank format tree like
 * "(ROOT (S (NP i) (VP (V asked) (NP her) (NP (DT a) (N question)))))".
 * If we also set include_align_index=true, we include source-side alignments
 * for each internal node of the tree.
 * <p>
 * So, if the source input sentence is "je lui ai pose un question", if we
 * turn on both configuration options, we end up with a decorated tree like
 * this:
 * "(ROOT{0-6} (S{0-6} (NP{0-1} i) (VP{1-6} (V{2-4} asked) (NP{1-2} her)
 * (NP{4-6} (DT{4-5} a) (N{5-6} question)))))".
 * <p>
 * This class contains all the information of that flat string representation:
 * the tree structure, the output (English) words, and the alignments to a
 * source sentence.
 * <p>
 * Using a Tree the source sentence it was aligned to, we can create
 * a DerivationTree object suitable for display. 
 *
 * @author Jonny Weese <jonny@cs.jhu.edu>
 */
public class Tree {

	/**
	 * An array holding the label of each node of the tree, in depth-first order.
	 * The label of a node means the NT label assigned to an internal node, or
	 * the terminal symbol (English word) at a leaf.
	 */
	private final String [] labels;

	/**
	 * The number of children of each node of the tree, in depth-first order.
	 */
	private final int [] numChildren;

	/**
	 * The smallest source-side index that each node covers, in depth-first order.
	 * Note that we only have this information for internal nodes. For leaves,
	 * this value will always be -1.
	 */
	private final int [] sourceStartIndices;

	/**
	 * 1 + the largest source-side index that each node covers, in depth-first
	 * order. Note that we only have this informaion for internal nodes. For
	 * leaves, this value will always be -1.
	 */
	private final int [] sourceEndIndices;

	/**
	 * A pattern to match an aligned internal node and pull out its information.
	 * This pattern matches:
	 *
	 * 1) start-of-string
	 * 2) (
	 * 3) an arbitrary sequence of non-whitespace characters (at least 1)
	 * 4) {
	 * 5) a decimal number
	 * 6) -
	 * 7) a decimal number
	 * 8) }
	 * 9) end-of-string
	 *
	 * That is, it matches something like "(FOO{32-55}". The string and two 
	 * decimal numbers (parts 3, 5, and 7) are captured in groups.
	 */
	private static final Pattern NONTERMINAL_PATTERN =
		Pattern.compile("^\\((\\S+)\\{(\\d+)-(\\d+)\\}$");

	/**
	 * Creates a Tree object from an input string in Penn treebank format with
	 * source alignment annotations.
	 */
	public Tree(String s) {
		final String [] tokens = s.replaceAll("\\)", " )").split("\\s+");
		int numNodes = 0;
		for (String t : tokens) {
			if (!t.equals(")")) {
				numNodes++;
			}
		}
		labels = new String[numNodes];
		numChildren = new int[numNodes];
		sourceStartIndices = new int[numNodes];
		sourceEndIndices = new int[numNodes];
		try {
			initialize(tokens);
		} catch (Exception e) {
			// This will catch most formatting errors.
			throw new IllegalArgumentException(
					String.format("couldn't create tree from string: \"%s\"", s),
					e);
		}
	}

	private void initialize(String [] tokens) {
		final Stack<Integer> stack = new Stack<Integer>();
		int nodeIndex = 0;
		for (String token : tokens) {
			final Matcher matcher = NONTERMINAL_PATTERN.matcher(token);
			if (matcher.matches()) {
				// new non-terminal node
				labels[nodeIndex] = matcher.group(1);
				sourceStartIndices[nodeIndex] = Integer.parseInt(matcher.group(2));
				sourceEndIndices[nodeIndex] = Integer.parseInt(matcher.group(3));
				stack.push(nodeIndex);
				nodeIndex++;
			} else if (token.equals(")")) {
				// finished a subtree
				stack.pop();
				if (stack.empty()) {
					break;
				} else {
					numChildren[stack.peek()]++;
				}
			} else {
				// otherwise, it's a new leaf node
				labels[nodeIndex] = token;
				sourceStartIndices[nodeIndex] = -1;
				sourceEndIndices[nodeIndex] = -1;
				numChildren[stack.peek()]++;
				nodeIndex++;
			}
		}
		if (!stack.empty()) {
			// Not enough close-parentheses at the end of the tree.
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Return the number of nodes in this Tree.
	 */
	public int size() {
		return labels.length;
	}

	/**
	 * Get the root Node of this Tree.
	 */
	public Node root() {
		return new Node(0);
	}

	private List<Integer> childIndices(int index) {
		List<Integer> result = new ArrayList<Integer>();
		int remainingChildren = numChildren[index];
		int childIndex = index + 1;
		while (remainingChildren > 0) {
			result.add(childIndex);
			childIndex = nextSiblingIndex(childIndex);
			remainingChildren--;
		}
		return result;
	}

	private int nextSiblingIndex(int index) {
		int result = index + 1;
		int remainingChildren = numChildren[index];
		for (int i = 0; i < remainingChildren; i++) {
			result = nextSiblingIndex(result);
		}
		return result;
	}

	public String yield() {
		String result = "";
		for (int i = 0; i < labels.length; i++) {
			if (numChildren[i] == 0) {
				if (!result.equals("")) {
					result += " ";
				}
				result += labels[i];
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return root().toString();
	}

	/**
	 * A class representing the Nodes of a tree.
	 */
	public class Node {

		/**
		 * The index into the Tree class's internal arrays.
		 */
		private final int index;

		private Node(int i) {
			index = i;
		}

		/**
		 * Get the label for this node. If the node is internal to the tree, its
		 * label is the non-terminal label assigned to it. If it is a leaf node,
		 * the label is the English word at the leaf.
		 */
		public String label() {
			return labels[index];
		}

		public boolean isLeaf() {
			return numChildren[index] == 0;
		}

		public int sourceStartIndex() {
			return sourceStartIndices[index];
		}

		public int sourceEndIndex() {
			return sourceEndIndices[index];
		}

		public List<Node> children() {
			List<Node> result = new ArrayList<Node>();
			for (int j : childIndices(index)) {
				result.add(new Node(j));
			}
			return result;
		}

		@Override
		public String toString() {
			if (isLeaf()) {
				return label();
			}
			String result = String.format("(%s{%d-%d}",
					                          label(),
																		sourceStartIndex(),
																		sourceEndIndex());
			for (Node c : children()) {
				result += String.format(" %s", c);
			}
			return result + ")";
		}
	}

	public static class NodeSourceStartComparator implements Comparator<Node> {
		public int compare(Node a, Node b) {
			return a.sourceStartIndex() - b.sourceStartIndex();
		}
	}
}
