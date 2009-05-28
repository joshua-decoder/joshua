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
package joshua.decoder.segment_file.sax_parser;

import joshua.decoder.segment_file.TypeCheckingException;
import joshua.decoder.segment_file.Segment;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.util.Regex;

import java.util.LinkedList;
import java.util.List;
import java.util.BitSet;

/**
 * Parsing state for partial Segment objects.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
class SAXSegment {
	private final String id;
	private final List<SAXConstraintSpan> spans;
	
	public SAXSegment(String id) {
		this.id    = id;
		this.spans = new LinkedList<SAXConstraintSpan>();
	}
	
	
	public void addSpan(SAXConstraintSpan span) {
		this.spans.add(span);
	}
	
	
	/**
	 * Verify type invariants for Segment. Namely, ensure that
	 * there are no overlapping hard spans.
	 * <p>
	 * This method also ensures, recursively, that the
	 * ConstraintSpans (and thus ConstraintRules) are also
	 * type-correct.
	 */
	public Segment typeCheck(String text) throws TypeCheckingException {
		
		final String id       = this.id;
		final String sentence = Regex.spaces.replaceAll(text, " ").trim();
		
		final List<ConstraintSpan> spans = new LinkedList<ConstraintSpan>();
		for (SAXConstraintSpan span : this.spans) {
			spans.add(span.typeCheck(sentence));
		}
		
		
		// TODO: this bit-vector should be exposed through the Segment interface (without mutability!) since Chart will want to construct one anyways for ensuring proper implementation of hard constraints for overlapping grammar rules
		BitSet hardSpans = new BitSet();
		for (ConstraintSpan span : spans) {
			if (span.isHard()) {
				if (hardSpans.get(span.start(),span.end()).isEmpty()) {
					hardSpans.set(span.start(),span.end());
				} else {
					// TODO: give a better error message. Line/column info can be provided by the caller; we'd want to provide info about the segment id, which spans overlap, the index range of overlap, etc. We'll need more than a bit-vector to figure some of that out though...
					throw new TypeCheckingException("Overlapping hard spans in segment " + id + ", approximately in the range of " + span.start() + " to " + span.end());
				}
			}
		}
		
		return new Segment() {
			public String id()                        { return id;       }
			public String sentence()                  { return sentence; }
			public List<ConstraintSpan> constraints() { return spans;    }
		};
	}
}
