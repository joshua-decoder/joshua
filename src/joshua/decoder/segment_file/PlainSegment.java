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

import joshua.util.NullIterator;

import java.util.Iterator;

/**
 * This class provides a trivial implementation of a Segment with
 * no constraint annotations.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class PlainSegment implements Segment {
	private static final NullIterator<ConstraintSpan>
		constraints = new NullIterator<ConstraintSpan>();
	
	private final String id;
	private final String sentence;
	
	public PlainSegment(String id, String sentence) {
		this.id = id;
		this.sentence = sentence;
	}
	
	public String id() { return this.id; }
	
	public String sentence() { return this.sentence; }
	
	public Iterator<ConstraintSpan> constraints() { return this.constraints; }
}
