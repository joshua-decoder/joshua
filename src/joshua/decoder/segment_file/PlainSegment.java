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
import java.util.LinkedList;

/**
 * This class provides a trivial implementation of a Segment with
 * no constraint annotations.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
class PlainSegment implements Segment {
	
	/* FIXME: Zhifei demanded to be able to have Lists instead
	 * of Iterators despite the fact that he should not be
	 * holding onto them. That means we can no longer trust the
	 * Chart to treat this class immutably since he may decide
	 * to add things to the constraints list. Which means we
	 * can no longer use the singleton pattern and allocate one
	 * static joshua.util.NullIterator object for all instances
	 * to share. Instead, we now have to allocate an empty
	 * list for every instance. What a waste.
	 */
	private final List<ConstraintSpan> constraints;
	
	private final String id;
	private final String sentence;
	
	public PlainSegment(String id, String sentence) {
		this.id = id;
		this.sentence = sentence;
		this.constraints = new LinkedList<ConstraintSpan>();
	}
	
	public String id() { return this.id; }
	
	public String sentence() { return this.sentence; }
	
	public List<ConstraintSpan> constraints() { return this.constraints; }
}
