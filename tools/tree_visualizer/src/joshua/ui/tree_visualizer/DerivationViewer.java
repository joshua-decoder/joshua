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
package joshua.decoder;

import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.*;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

import org.apache.commons.collections15.Transformer;

public class DerivationViewer extends VisualizationViewer<Node,DerivationTreeEdge> {
	public static final int DEFAULT_HEIGHT = 500;
	public static final int DEFAULT_WIDTH = 500;
	public static final Color SRC = Color.WHITE;
	public static final Color TGT = Color.RED;

	public DerivationViewer(DerivationTree g)
	{
		super(new CircleLayout(g));
		DelegateTree del = new DelegateTree(g);
		del.setRoot(g.getRoot());
		setGraphLayout(new TreeLayout(del, 100));
		setPreferredSize(new Dimension(DEFAULT_HEIGHT, DEFAULT_WIDTH));
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller());

		DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		setGraphMouse(graphMouse);

		getRenderContext().setVertexFillPaintTransformer(vp);
		getRenderContext().setEdgeStrokeTransformer(es);
		getRenderContext().setVertexShapeTransformer(ns);
		getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}

	private static Transformer<Node,Paint> vp = new Transformer<Node,Paint>() {
		public Paint transform(Node n)
		{
			if (n.isSource())
				return SRC;
			else
				return TGT;
		}
	};

	private static Transformer<DerivationTreeEdge,Stroke> es = new Transformer<DerivationTreeEdge,Stroke>() {
		public Stroke transform(DerivationTreeEdge e)
		{
			if (e.pointsToSource())
				return new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{ 10.0f }, 0.0f);
			else
				return new BasicStroke(1.0f);
		}
	};

	private static Transformer<Node,Shape> ns = new Transformer<Node,Shape>() {
		public Shape transform(Node n)
		{
			double len = 10.0 * n.toString().length();
			double margin = 5.0;
			return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
		}
	};
}
