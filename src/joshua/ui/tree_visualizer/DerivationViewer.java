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

import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.*;

import javax.swing.JLabel;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

import org.apache.commons.collections15.Transformer;

public class DerivationViewer extends VisualizationViewer<Node,DerivationTreeEdge> {
	public static final int DEFAULT_HEIGHT = 500;
	public static final int DEFAULT_WIDTH = 500;
	public static final Color SRC = Color.WHITE;
	private Color TGT;
	
	public static final Color HIGHLIGHT = Color.pink;
	
	public static enum AnchorType { ANCHOR_ROOT, ANCHOR_LEFTMOST_LEAF };
	
	private AnchorType anchorStyle;
	private Point2D anchorPoint;

	public DerivationViewer(DerivationTree g, Dimension d, Color targetColor, AnchorType anchor)
	{
		super(new CircleLayout<Node,DerivationTreeEdge>(g));
		anchorStyle = anchor;
		DerivationTreeTransformer dtt = new DerivationTreeTransformer(g, d, false);
		StaticLayout<Node,DerivationTreeEdge> derivationLayout = new StaticLayout<Node,DerivationTreeEdge>(g, dtt);
//		derivationLayout.setSize(dtt.getSize());
		setGraphLayout(derivationLayout);
		scaleToLayout(new LayoutScalingControl());
//		g.addCorrespondences();
		setPreferredSize(new Dimension(DEFAULT_HEIGHT, DEFAULT_WIDTH));
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Node>());

		DefaultModalGraphMouse<Node,DerivationTreeEdge> graphMouse = new DefaultModalGraphMouse<Node,DerivationTreeEdge>();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		setGraphMouse(graphMouse);
		addKeyListener(graphMouse.getModeKeyListener());
		this.setPickedVertexState(new DerivationTreePickedState(g));

		getRenderContext().setVertexFillPaintTransformer(vp);
		getRenderContext().setEdgeStrokeTransformer(es);
		getRenderContext().setVertexShapeTransformer(ns);
		getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		
		TGT = targetColor;
		anchorPoint = dtt.getAnchorPosition(anchorStyle);
	}
	
	public void setGraph(DerivationTree tree)
	{
		DerivationTreeTransformer dtt = new DerivationTreeTransformer(tree, getSize(), true);
		dtt.setAnchorPoint(anchorStyle, anchorPoint);
		setGraphLayout(new StaticLayout<Node,DerivationTreeEdge>(tree, dtt));
	}

	private Transformer<Node,Paint> vp = new Transformer<Node,Paint>() {
		public Paint transform(Node n)
		{
			if (n.isHighlighted())
				return HIGHLIGHT;
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
			JLabel x = new JLabel();
			double len = x.getFontMetrics(x.getFont()).stringWidth(n.toString());
			double margin = 5.0;
			return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
		}
	};
}
