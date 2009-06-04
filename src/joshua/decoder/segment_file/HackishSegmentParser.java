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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.IOException;

/**
 * This parser recognizes an ad hoc format that mixes an SGML-like
 * format with a one-sentence-per-line format, both with no
 * annotations.
 *
 * @deprecated This is provided only for backward compatibility and
 * should be removed in future releases. {@link PlainSegmentParser}
 * (or {@link joshua.decoder.segment_file.sax_parser.SAXSegmentParser})
 * should be used instead.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
@Deprecated
public class HackishSegmentParser implements SegmentFileParser {
	protected final int startSegmentID;
	protected       int currentSegmentID;
	
	protected static final Pattern SEG_START =
				Pattern.compile("^\\s*<seg\\s+id=\"(\\d+)\"[^>]*>\\s*");
	protected static final Pattern SEG_END =
				Pattern.compile("\\s*</seg\\s*>\\s*$");
	
	public HackishSegmentParser() {
		this.startSegmentID = -1;
	}
	
	public HackishSegmentParser(int startSegmentID) {
		// We subtract one because of the order in which
		// we increment vs return the value to callers.
		this.startSegmentID = startSegmentID - 1;
	}
	
	
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
		
		try {
			LineReader reader = new LineReader(in);
			this.currentSegmentID = this.startSegmentID;
			try {
				while (reader.hasNext()) {
					String cleanedSentence =
						Regex.spaces.replaceAll(reader.next(), " ").trim();
					
					Matcher m = SEG_START.matcher(cleanedSentence);
					
					final String sentence;
					if (m.find()) {
						String id = m.group(1);
						if (null == id) {
							throw new RuntimeException("Something strange happened with regexes. This is a bug.");
						} else {
							this.currentSegmentID = Integer.parseInt(id);
							sentence =
								SEG_END.matcher(m.replaceFirst("")).replaceAll("");
						}
					} else {
						this.currentSegmentID++;
						sentence = cleanedSentence;
					}
					
					coit.coNext(new PlainSegment(
						Integer.toString(this.currentSegmentID),
						sentence));
				}
			} finally {
				reader.close();
			}
		} finally {
			coit.finish();
		}
	}
}
