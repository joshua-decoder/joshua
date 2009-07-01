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

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;

import java.awt.Dimension;
import java.awt.geom.Point2D;

public class DerivationTreeTransformer implements Transformer<Node,Point2D> {
	private TreeLayout<Node,DerivationTreeEdge> treeLayout;
	private DerivationTree graph;
	private Node root;
	private Node sourceRoot;

	private double Y_DIST;
	private double X_DIST;

	public DerivationTreeTransformer(DerivationTree t, Dimension d)
	{
		graph = t;
		DelegateForest<Node,DerivationTreeEdge> del = new DelegateForest<Node,DerivationTreeEdge>(t);
		del.setRoot(t.getRoot());
		del.setRoot(t.getSourceRoot());
		root = t.getRoot();
		sourceRoot = t.getSourceRoot();
		Y_DIST = d.getHeight() / (2 * (1 + distanceToLeaf(root)));
		int leafCount = 0;
		for (Node n : t.getVertices()) {
			if (t.outDegree(n) == 0)
				leafCount++;
		}
		X_DIST = d.getWidth() / leafCount;
		
		treeLayout = new TreeLayout<Node,DerivationTreeEdge>(del, (int) Math.round(X_DIST));
	}

	public Point2D transform(Node n)
	{
		double x, y;
		Point2D t = treeLayout.transform(n);
		if (n.isSource()) {
			x = /*treeLayout.transform(root).getX() +*/ (t.getX() - treeLayout.transform(sourceRoot).getX() + treeLayout.transform(root).getX());
			y = Y_DIST * (distanceToLeaf(n) + 1);
		}
		else {
			x = t.getX();
			y = Y_DIST * (-1) * distanceToLeaf(n);
		}
		return new Point2D.Double(x, y + Y_DIST * (1 + distanceToLeaf(root)));
	}

	private int distanceToLeaf(Node n)
	{
		if (graph.getSuccessors(n).isEmpty())
			return 0;
		int result = 0;
		for (Object x : graph.getSuccessors(n)) {
			int tmp = distanceToLeaf((Node) x);
			if (tmp > result)
				result = tmp;
		}
		return 1 + result;
	}
	
	public Dimension getSize()
	{
		int height = (int) Math.round(2 * Y_DIST * (1 + distanceToLeaf(root)));
		int width = (int) Math.round(2 * treeLayout.transform(root).getX());
		Dimension ret = new Dimension(width, height);
		return ret;
	}
}
