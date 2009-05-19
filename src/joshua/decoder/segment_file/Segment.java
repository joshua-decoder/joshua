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
package joshua.decoder.segment_file;

import java.util.Iterator;

/**
 * This interface represents an individual segment for translation,
 * corresponding with a single {@link joshua.decoder.chart_parser.Chart}.
 * Each segment contains approximately once sentence or one utterance,
 * and can additionally contain some constraints representing initial
 * items for seeding the chart.
 *
 * @author wren ng thornton
 */
public interface Segment {
	
	/**
	 * Return the sentence to be translated. The client of this
	 * interface bears the responsibility to tokenize on
	 * whitespace and to integerize the words.
	 */
	String sentence();
	
	
	// TODO: maybe we should return a smarter collection like
	// a map indexed on start and/or stop indices. That's up
	// to the Chart constructors.
	/**
	 * Return a collection of all constraints associated with
	 * this segment.
	 */
	Iterator<ConstraintSpan> constraints();
}
