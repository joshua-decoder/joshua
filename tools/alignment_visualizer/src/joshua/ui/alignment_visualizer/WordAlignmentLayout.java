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

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;

import java.util.HashMap;
import java.awt.geom.*;

public class WordAlignmentLayout extends StaticLayout<Word,Integer> {
	
	private WordAlignmentLayout(Graph<Word,Integer> g, Transformer<Word,Point2D> vp)
	{
		super(g, vp);
	}

	public static WordAlignmentLayout makeWordAlignmentLayout(Graph<Word,Integer> g, double height, double spacing)
	{
		Transformer<Word,Point2D> vp = calculateInitializer(g, height, spacing);
		return new WordAlignmentLayout(g, vp);
	}

	private static Transformer<Word,Point2D> calculateInitializer(Graph<Word,Integer> g, double height, double spacing)
	{
		HashMap<Word,Point2D> map = new HashMap<Word,Point2D>();
		for (Word w : g.getVertices()) {
			double x, y;
			if (w.isSource())
				y = 0.0;
			else
				y = height;
			x = spacing * w.position();
			map.put(w, new Point2D.Double(x, y));
		}
		return TransformerUtils.mapTransformer(map);
	}
}
