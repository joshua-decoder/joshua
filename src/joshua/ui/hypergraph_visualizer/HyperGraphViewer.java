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

import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.*;

import javax.swing.JLabel;
import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

import org.apache.commons.collections15.Transformer;

import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.hypergraph.*;
import joshua.decoder.ff.tm.BilingualRule;

import java.util.ArrayList;
import java.util.HashMap;

public class HyperGraphViewer extends VisualizationViewer<Vertex,Edge> {
	public static final int DEFAULT_HEIGHT = 500;
	public static final int DEFAULT_WIDTH = 500;
	public static final Color SRC = Color.WHITE;
	public static final Color TGT = Color.RED;
	
	public static final String USAGE = "USAGE: HyperGraphViewer <items file> <rules file> <first sentence> <last sentence>";

	static Vocabulary vocab;
	
	public HyperGraphViewer(JungHyperGraph g)
	{
//		super(new StaticLayout(g, new HyperGraphTransformer(g)));
		super(new DAGLayout<Vertex,Edge>(g));
//		DelegateTree<Vertex,Edge> gtree = new DelegateTree<Vertex,Edge>(g);
//		gtree.setRoot(g.getRoot());
//		setGraphLayout(new TreeLayout<Vertex,Edge>(gtree));
//		setGraphLayout(new StaticLayout<Vertex,Edge>(g, new HyperGraphTransformer(g)));
//		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Vertex>());
		setVertexToolTipTransformer(new ToStringLabeller<Vertex>());

		DefaultModalGraphMouse<Vertex,Edge> graphMouse = new DefaultModalGraphMouse<Vertex,Edge>();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		setGraphMouse(graphMouse);

		getRenderContext().setVertexFillPaintTransformer(vp);
	//	getRenderContext().setEdgeStrokeTransformer(es);
		getRenderContext().setVertexShapeTransformer(ns);
	//	getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}

	private static Transformer<Vertex,Paint> vp = new Transformer<Vertex,Paint>() {
		public Paint transform(Vertex v)
		{
		/*
			if (n.isSource())
				return SRC;
			else
				return TGT;
		*/
			return Color.BLUE;
		}
	};

	private static Transformer<Edge,Stroke> es = new Transformer<Edge,Stroke>() {
		public Stroke transform(Edge e)
		{
			if (e.isHighlighted())
				return new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{ 10.0f }, 0.0f);
			else
				return new BasicStroke(1.0f);
		}
	};

	private static Transformer<Vertex,Shape> ns = new Transformer<Vertex,Shape>() {
		public Shape transform(Vertex v)
		{
		//	JLabel x = new JLabel();
			double len = 20; //x.getFontMetrics(x.getFont()).stringWidth(n.toString());
			double margin = 5.0;
			if (v.isHGNode())
				return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
			else
				return new Ellipse2D.Double(-10, -10, 20, 20);
		}
	};

	public static void main(String [] argv)
	{
		if (argv.length < 4) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String itemsFile = argv[0];
		String rulesFile = argv[1];
		int firstSentence = Integer.parseInt(argv[2]);
		int lastSentence = Integer.parseInt(argv[3]);
		HashMap<Integer,Integer> chosenSentences = new HashMap<Integer,Integer>();
		for (int i = firstSentence; i < lastSentence; i++) {
			chosenSentences.put(i, i);
		}
		vocab = new Vocabulary();
		DiskHyperGraph dhg = new DiskHyperGraph(vocab, 0, true, null);
		dhg.initRead(itemsFile, rulesFile, chosenSentences);
		JungHyperGraph hg = new JungHyperGraph(dhg.readHyperGraph());
		JFrame frame = new JFrame("Joshua Hypergraph");
		frame.getContentPane().add(new HyperGraphViewer(hg));
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		hg.addNode(hg.getRoot().node(), null, 0, 0);
		return;
	}
}
