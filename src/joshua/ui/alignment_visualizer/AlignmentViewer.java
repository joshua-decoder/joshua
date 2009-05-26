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
package joshua.ui.alignment_visualizer;


import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

import org.apache.commons.collections15.Transformer;

public class AlignmentViewer extends VisualizationViewer<Word,Integer> {
	public static final int DEFAULT_HEIGHT = 500;
	public static final int DEFAULT_WIDTH = 500;
	public static final Color SRC = Color.WHITE;
	public static final Color TGT = Color.RED;

	public AlignmentViewer(WordAlignmentGraph g)
	{
		super(WordAlignmentLayout.makeWordAlignmentLayout(g, 100, 80));
		setPreferredSize(new Dimension(DEFAULT_HEIGHT, DEFAULT_WIDTH));
		getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Word>());

		DefaultModalGraphMouse<Word,Integer> graphMouse = new DefaultModalGraphMouse<Word,Integer>();
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		setGraphMouse(graphMouse);

		getRenderContext().setVertexFillPaintTransformer(vp);
		getRenderContext().setVertexShapeTransformer(ns);
		getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}

	private static Transformer<Word,Paint> vp = new Transformer<Word,Paint>() {
		public Paint transform(Word w)
		{
			if (w.isSource())
				return SRC;
			else
				return TGT;
		}
	};

	private static Transformer<Word,Shape> ns = new Transformer<Word,Shape>() {
		public Shape transform(Word n)
		{
			double len = 10.0 * n.toString().length();
			double margin = 5.0;
			return new Rectangle2D.Double((len + margin) / (-2), 0, len + 2 * margin, 20);
		}
	};
}
