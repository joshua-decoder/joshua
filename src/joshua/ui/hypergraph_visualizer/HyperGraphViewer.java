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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

public class HyperGraphViewer extends VisualizationViewer<Vertex,Edge> {
	public static final int DEFAULT_HEIGHT = 500;
	public static final int DEFAULT_WIDTH = 500;
	public static final Color SRC = Color.WHITE;
	public static final Color TGT = Color.RED;
	
	public static final String USAGE = "USAGE: HyperGraphViewer <items file> <rules file> <first sentence> <last sentence>";
	public static final double EDGE_ELLIPSE_SIZE = 10;
	
	JungHyperGraph graph;
	
	JList edgeList;
	
	public HyperGraphViewer(JungHyperGraph g)
	{
		super(new StaticLayout<Vertex,Edge>(g, new HyperGraphTransformer(g)));
		this.graph = g;
		this.edgeList = new JList(new DefaultListModel());
		this.edgeList.setCellRenderer(new HyperEdgeListCellRenderer());
		this.edgeList.addListSelectionListener(new HyperEdgeListSelectionListener(this));
//		super(new DAGLayout<Vertex,Edge>(g));
//		DelegateTree<Vertex,Edge> gtree = new DelegateTree<Vertex,Edge>(g);
//		gtree.setRoot(g.getRoot());
//		setGraphLayout(new TreeLayout<Vertex,Edge>(gtree));
//		setGraphLayout(new StaticLayout<Vertex,Edge>(g, new HyperGraphTransformer(g)));
		getRenderContext().setVertexLabelTransformer(toStringTransformer());
//		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Vertex>());
		setVertexToolTipTransformer(toolTipTransformer());

		DefaultModalGraphMouse<Vertex,Edge> graphMouse = new DefaultModalGraphMouse<Vertex,Edge>();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		this.setPickedVertexState(new HyperGraphPickedState(this));
		setGraphMouse(graphMouse);
		addKeyListener(graphMouse.getModeKeyListener());

		getRenderContext().setVertexFillPaintTransformer(vertexPainter());
	//	getRenderContext().setEdgeStrokeTransformer(es);
		getRenderContext().setVertexShapeTransformer(ns);
	//	getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}
	
	private Transformer<Vertex,String> toStringTransformer() {
		return new Transformer<Vertex,String>() {
			public String transform(Vertex v) {
				if (v instanceof LeafVertex) {
					return Vocabulary.word(((LeafVertex) v).getEnglish());
				}
				else if (v instanceof NodeVertex) {
					String nt = Vocabulary.word(((NodeVertex) v).getNode().lhs);
					return String.format("%s{%d-%d}", nt, ((NodeVertex) v).getNode().i, ((NodeVertex) v).getNode().j);
				}
				else {
					Rule r = ((HyperEdgeVertex) v).getHyperEdge().getRule();
					if (r != null) {
						String lhs = Vocabulary.word(r.getLHS());
						String french = Vocabulary.getWords(r.getFrench());
						String english = Vocabulary.getWords(r.getEnglish());
						return String.format("%s -> { %s ; %s }", lhs, french, english);
					}
					else
						return "";
				}
			}
		};
	}
	
	private Transformer<Vertex,String> toolTipTransformer() {
		return new Transformer<Vertex,String>() {
			public String transform(Vertex v)
			{
				if (v instanceof HyperEdgeVertex) {
					NodeVertex pred = (NodeVertex) graph.getPredecessors(v).toArray()[0];
					int otherEdges = pred.getNode().hyperedges.size() - 1;
					return String.format("%d other edges", otherEdges);
				}
				else if (v instanceof NodeVertex) {
					// TODO: Returns meaningless hash rather than a serialization.
					return String.valueOf(((NodeVertex) v).getNode().hashCode());
				}
				else {
					return "";
				}
			}
		};
	}

	private Transformer<Vertex,Paint> vertexPainter() {
		return new Transformer<Vertex,Paint>() {
			private Color [] colors = { Color.blue, Color.red, Color.yellow, Color.green, Color.cyan };
			public Paint transform(Vertex v)
			{
				return colors[v.getColor() % colors.length];
			}
		};
	}

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
			if (v instanceof NodeVertex || v instanceof LeafVertex)
				return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
			else
				return new Ellipse2D.Double(-.5 * EDGE_ELLIPSE_SIZE, -.5 * EDGE_ELLIPSE_SIZE, EDGE_ELLIPSE_SIZE, EDGE_ELLIPSE_SIZE);
		}
	};
	
	public static void visualizeHypergraphInFrame(HyperGraph hg)
	{
		JFrame frame = new JFrame("Joshua Hypergraph");
		frame.setLayout(new BorderLayout());
		HyperGraphViewer vv = new HyperGraphViewer(new JungHyperGraph(hg));
		
		frame.getContentPane().add(vv, BorderLayout.CENTER);
		frame.getContentPane().add(new JScrollPane(vv.edgeList), BorderLayout.WEST);
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		return;
	}

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
		DiskHyperGraph dhg = new DiskHyperGraph(0, true, null);
		dhg.initRead(itemsFile, rulesFile, chosenSentences);
		JungHyperGraph hg = new JungHyperGraph(dhg.readHyperGraph());
		JFrame frame = new JFrame("Joshua Hypergraph");
		frame.getContentPane().add(new HyperGraphViewer(hg));
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		return;
	}
}
