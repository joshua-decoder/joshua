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
 * This interface represents a collection of constraints for a given
 * span in the associated segment. Intuitively, each constraint
 * corresponds to one or more items in the chart for parsing, except
 * that we pre-seed the chart with these items before beginning the
 * parsing algorithm. Some constraints can be "hard", in which case
 * the regular grammar is not consulted for these spans. It is an
 * error to have hard constraints for overlapping spans.
 *
 * Indices for the span boundaries mark the transitions between
 * words. Thus, the 0 index occurs before the first word, the 1
 * index occurs between the first and second words, 2 is between
 * the second and third, etc. Consequently, it is an error for the
 * end index to be equal to or less than the start index. It is
 * also an error to have negative indices or to have indices larger
 * than the count of words in the segment. Clients may assume that
 * no <code>ConstraintSpan</code> objects are constructed which
 * violate these laws.
 *
 * @author wren ng thornton
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
	 * sets of RULE type constraints.
	 */
	boolean isHard();
	
	/**
	 * Return a collection of the "rules" for this constraint
	 * span.
	 */
	Iterator<ConstraintRule> rules();
}
