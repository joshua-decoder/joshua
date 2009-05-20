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

import joshua.util.io.LineReader;
import joshua.util.NullIterator;
import joshua.util.CoIterator;
import joshua.util.Regex;

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * This is a parser for plain files with one sentence per line and
 * no annotations.
 *
 * @author wren ng thornton
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class PlainSegmentParser implements SegmentFileParser {
	
//===============================================================
// SegmentFileParser Methods
//===============================================================

	public void parseSegmentFile(InputStream in, CoIterator<Segment> coit)
	throws IOException {
		if (null == coit) {
			// Maybe just return immediately instead?
			throw new IllegalArgumentException("null co-iterator");
		}
		if (null == in) {
			// Maybe just return immediately instead?
			throw new IllegalArgumentException("null input stream");
		}
		
		LineReader reader = new LineReader(in);
		try {
			while (reader.hasNext()) {
				final String sentence = 
					Regex.spaces.replaceAll(reader.next(), " ").trim();
				
				coit.coNext(new Segment() {
					public String sentence() {
						return sentence;
					}
					public Iterator<ConstraintSpan> constraints() {
						return new NullIterator<ConstraintSpan>();
					}
				});
			}
		} finally {
			reader.close();
		}
	}
}
