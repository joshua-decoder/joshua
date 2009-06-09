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

import java.awt.geom.Point2D;

public class HyperGraphTransformer implements Transformer<Vertex,Point2D> {
	private JungHyperGraph graph;
	private Vertex root;

	static int Y_DIST = 50;

	public HyperGraphTransformer(JungHyperGraph t)
	{
		graph = t;
		root = t.getRoot();
	}

	public Point2D transform(Vertex v)
	{
		double x, y;
		x = 0;
		y = Y_DIST * distanceToRoot(v);
		return new Point2D.Double(x, y);
	}

	private int distanceToRoot(Vertex v)
	{
		if (graph.getSuccessors(v).isEmpty())
			return 0;
		int result = 0;
		for (Object x : graph.getSuccessors(v)) {
			int tmp = distanceToRoot((Vertex) x);
			if (tmp > result)
				result = tmp;
		}
		return 1 + result;
	}
}
