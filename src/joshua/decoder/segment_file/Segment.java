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

import java.util.List;

/**
 * This interface represents an individual segment for translation,
 * corresponding with a single {@link joshua.decoder.chart_parser.Chart}.
 * Each segment contains approximately one sentence or one utterance,
 * and can additionally contain some constraints representing initial
 * items for seeding the chart.
 * <p>
 * The {@link Segment}, {@link ConstraintSpan}, and {@link ConstraintRule}
 * interfaces are for defining an interchange format between a
 * SegmentFileParser and the Chart class. These interfaces
 * <em>should not</em> be used internally by the Chart. The
 * objects returned by a SegmentFileParser will not be optimal for
 * use during decoding. The Chart should convert each of these
 * objects into its own internal representation during construction.
 * That is the contract described by these interfaces.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public interface Segment {
	
	/**
	 * Return the sentence ID.
	 */
	String id();
	
	
	/**
	 * Return the sentence to be translated. The client of this
	 * interface bears the responsibility to tokenize on
	 * whitespace and to integerize the words.
	 */
	String sentence();
	
	
	/**
	 * Return a collection of all constraints associated with
	 * this segment. Implementations of this interface should
	 * ensure that no overlapping hard spans are ever constructed.
	 * <p>
	 * This return type is suboptimal for some SegmentFileParsers.
	 * It should be an {@link java.util.Iterator} instead in
	 * order to reduce the coupling between this class and
	 * Chart. See the note above about the fact that this
	 * interface should not be used internally by the Chart
	 * class because it will not be performant.
	 */
	List<ConstraintSpan> constraints();
}
