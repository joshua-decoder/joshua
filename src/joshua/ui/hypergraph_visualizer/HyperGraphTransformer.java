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
import java.util.HashMap;

public class HyperGraphTransformer implements Transformer<Vertex,Point2D> {
	private JungHyperGraph graph;
	private Vertex root;
	private HashMap<Integer,Integer> verticesAtHeight;

	static int Y_DIST = 50;
	static int X_DIST = 50;

	public HyperGraphTransformer(JungHyperGraph t)
	{
		graph = t;
		root = t.getRoot();
		verticesAtHeight = new HashMap<Integer,Integer>();
		tabulateVertices();
	}
	
	public void tabulateVertices()
	{
		for (Vertex v : graph.getVertices()) {
			int height = v.height();
			Integer currHeight = verticesAtHeight.get(height);
			if (currHeight == null)
				verticesAtHeight.put(height, 1);
			else
				verticesAtHeight.put(height, currHeight + 1);
		}
		return;
	}

	public Point2D transform(Vertex v)
	{
		double x, y;
		x = X_DIST * v.width();
		y = Y_DIST * distanceToRoot(v);
		return new Point2D.Double(x, y);
	}

	private int distanceToRoot(Vertex v)
	{
		return v.height();
	}
}
