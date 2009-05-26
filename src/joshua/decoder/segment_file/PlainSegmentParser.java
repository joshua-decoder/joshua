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
import joshua.util.CoIterator;
import joshua.util.Regex;

import java.io.InputStream;
import java.io.IOException;

/**
 * This is a parser for plain files with one sentence per line and
 * no annotations.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
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
		
		// coit.coNext may throw unchecked exceptions, so
		// we try...finally to be sure we close the reader.
		// reader.close may throw IOException or unchecked,
		// so we try...finally to ensure that we finish the
		// coiterator.
		try {
			LineReader reader = new LineReader(in);
			int i = 0;
			try {
				while (reader.hasNext()) {
					
					coit.coNext(new PlainSegment(
						Integer.toString(i),
						Regex.spaces.replaceAll(reader.next(), " ").trim() ));
					
					++i;
				}
			} finally {
				reader.close();
			}
		} finally {
			coit.finish();
		}
	}
}
