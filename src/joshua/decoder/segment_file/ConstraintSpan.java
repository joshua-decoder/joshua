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
 * This interface represents a collection of constraints for a given
 * span in the associated segment. Intuitively, each constraint
 * corresponds to one or more items in the chart for parsing, except
 * that we pre-seed the chart with these items before beginning the
 * parsing algorithm. Some constraints can be "hard", in which case
 * the regular grammar is not consulted for these spans. It is an
 * error to have hard constraints for overlapping spans.
 * <p>
 * Indices for the span boundaries mark the transitions between
 * words. Thus, the 0 index occurs before the first word, the 1
 * index occurs between the first and second words, 2 is between
 * the second and third, etc. Consequently, it is an error for the
 * end index to be equal to or less than the start index. It is
 * also an error to have negative indices or to have indices larger
 * than the count of words in the segment. Clients may assume that
 * no <code>ConstraintSpan</code> objects are constructed which
 * violate these laws.
 * <p>
 * The {@link Segment}, {@link ConstraintSpan}, and {@link ConstraintRule}
 * interfaces are for defining an interchange format between a
 * SegmentFileParser and the Chart class. These interfaces
 * <emph>should not</emph> be used internally by the Chart. The
 * objects returned by a SegmentFileParser will not be optimal for
 * use during decoding. The Chart should convert each of these
 * objects into its own internal representation during construction.
 * That is the contract described by these interfaces.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public interface ConstraintSpan {
	
	/**
	 * Return the starting index of the span covered by this
	 * constraint.
	 */
	int start();
	
	/**
	 * Return the ending index of the span covered by this
	 * constraint. Clients may assume
	 * <code>this.end() &gt;= 1 + this.start()</code>.
	 */
	int end();
	
	/**
	 * Return whether this is a hard constraint which should
	 * override the grammar. This value only really matters for
	 * sets of <code>RULE</code> type constraints.
	 */
	boolean isHard();
	
	/**
	 * Return a collection of the "rules" for this constraint
	 * span.
	 * <p>
	 * This return type is suboptimal for some SegmentFileParsers.
	 * It should be an {@link java.util.Iterator} instead in
	 * order to reduce the coupling between this class and
	 * Chart. See the note above about the fact that this
	 * interface should not be used internally by the Chart
	 * class because it will not be performant.
	 */
	List<ConstraintRule> rules();
}
