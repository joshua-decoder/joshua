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
package joshua.ui.hypergraph_visualizer;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;

import java.awt.geom.Point2D;
import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;

public class HyperGraphTransformer implements Transformer<Vertex,Point2D> {
	private JungHyperGraph graph;
	private TreeLayout<Vertex,Edge> graphTree;

	static int Y_DIST = 50;
	static int X_DIST = 50;
	static int Y_BASELINE = 500;

	public HyperGraphTransformer(JungHyperGraph t)
	{
		graph = t;
		DelegateTree<Vertex,Edge> tree = new DelegateTree<Vertex,Edge>(t);
		tree.setRoot(t.getRoot());
		graphTree = new TreeLayout<Vertex,Edge>(tree);
	}

	public Point2D transform(Vertex v)
	{
	//	System.err.print(v);
		double x, y;
	/*
		if (v instanceof NodeVertex) {
			NodeVertex nv = (NodeVertex) v;
			int span = nv.getNode().j - nv.getNode().i;
			y = Y_BASELINE - Y_DIST * distanceToLeaf(v);
			x = X_DIST * nv.getNode().i;
			x += .5 * X_DIST * span;
		}
		else if (v instanceof LeafVertex) {
			LeafVertex lv = (LeafVertex) v;
			y = Y_BASELINE;
			x = X_DIST * lv.getTargetPosition();
		}
		else {
			Vertex node = (Vertex) graph.getPredecessors(v).toArray()[0];
			Point2D nodePosition = transform(node);
			x = nodePosition.getX();
			y = nodePosition.getY() + .5 * Y_DIST;
		}
	*/
	//	System.err.println(": (" + x + "," + y + ")");
		Point2D treePosition = graphTree.transform(v);
		x = treePosition.getX();
		y = Y_BASELINE - Y_DIST * distanceToLeaf(v);
		return new Point2D.Double(x, y);
	}
	
	private int distanceToLeaf(Vertex v)
	{
		if (graph.getSuccessorCount(v) == 0)
			return 0;
		int maxDistance = 0;
		for (Vertex successor : graph.getSuccessors(v)) {
			int dist = distanceToLeaf(successor);
			if (dist > maxDistance)
				maxDistance = dist;
		}
		return 1 + maxDistance;
	}

}
