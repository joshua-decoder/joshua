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

import joshua.util.CoIterator;

import java.io.InputStream;
import java.io.IOException;


/**
 * This interface defines a parser of segment files. This is used
 * by the main decoder loop (in DecoderThread) to enumerate the
 * segments for translation.
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
public interface SegmentFileParser {
	
	/**
	 * Parse an InputStream to produce a sequence of Segments,
	 * and use the co-iterator to consume them. We use co-iterators
	 * instead of iterators because some parsers are difficult
	 * to interrupt, and it can take too much memory to hold
	 * the whole file in memory during transation of each
	 * segment.
	 * <p>
	 * This method shall close the InputStream before exiting,
	 * since it consumes the entirety of the stream.
	 */
	void parseSegmentFile(InputStream in, CoIterator<Segment> coit)
	throws IOException;
	
	// TODO: allow throwing java.text.ParseException as well, to make parsing errors standard rather than requiring IOException wrapping (ParseException wrapping is less wrong-headed)
	
	/* SAXParser also supports java.io.File, org.xml.sax.InputSource,
	 * and String URI in its parse methods. We use InputStream
	 * because it's the most general and it allows for getting
	 * inputs from places other than the file system.
	 * 
	 * To resolve the hack in LineReader, we may consider using
	 * File instead or in addition to InputStream. An ugly
	 * solution, but maybe less ugly than the LineReader hack.
	 */
}
