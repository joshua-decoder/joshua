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

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is for providing line and column information about
 * the current location in an InputStream.
 * <p>
 * This is primarily designed for use by {@link org.xml.sax.Locator}
 * so we use the same definition of line breaks as in The XML
 * specification. Namely, the two-character sequence
 * <code>#xD #xA</code> and the character <code>#xD</code> when
 * not followed by a <code>#xA</code> character, are assumed to
 * be "translated" into a single <code>#xA</code> character which
 * represents the new line.
 *
 * @see <a href="http://www.w3.org/TR/REC-xml/">Extensible Markup Language (XML) 1.0 (Fifth Edition)</a>
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class LocatorInputStream extends InputStream {
	private final InputStream in;
	
	private byte previous = -1;
	// BUG: we need unbounded integers...
	private int line     = 0;
	private int column   = 0;
	
	/**
	 * Wrap an InputStream in order to track location information.
	 * This can be combined with other feature wrappers like
	 * BufferedInputStream.
	 */
	public LocatorInputStream(InputStream in) {
		if (null == in) {
			throw new NullPointerException();
		}
		this.in = in;
	}
	
	
//===============================================================
// Delegated Methods
//===============================================================
	
	public int available() throws IOException {
		return this.in.available();
	}
	
	public void close() throws IOException {
		this.in.close();
	}
	
	public void mark(int readlimit) {
		this.in.mark(readlimit);
	}
	
	public boolean markSupported() {
		return this.in.markSupported();
	}
	
	public void reset() throws IOException {
		this.in.reset();
	}
	
	public long skip(long n) throws IOException {
		return this.in.skip(n);
	}
	
//===============================================================
// Read Methods
//===============================================================
	
	/** Blockingly read a single byte (and cast to int). */
	public int read() throws IOException {
		int ch = this.in.read();
		if (ch < 0) {
			// End of stream
		} else {
			checkForNewline((byte)ch);
		}
		return ch;
	}
	
	
	/** Read into a buffer. */
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	
	/**
	 * Read up to <code>len</code> bytes into a buffer starting
	 * at index <code>off</code>.
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int bytesRead = this.in.read(b, off, len);
		if (bytesRead > 0) {
			// FIXME: this is woefully inefficient.
			for (int i = off; i < off+bytesRead; ++i) {
				checkForNewline(b[i]);
			}
		}
		return bytesRead;
	}
	
//===============================================================
// Helper method for maintaining locator state
//===============================================================
	
	private void checkForNewline(byte ch) {
		if (this.previous < 0) {
			this.previous = ch;
		
		} else {
			if (this.previous == 0xD || this.previous == 0xA) {
				this.line++;
				this.column = 0;
			} else {
				this.column++;
			}
			
			if (this.previous == 0xD && ch == 0xA) {
				this.previous = -1;
			} else {
				this.previous = ch;
			}
		}
	}
	
//===============================================================
// Locator interface methods (some of them)
//===============================================================
	
	public int getLineNumber() {
		return this.line;
	}
	
	public int getColumnNumber() {
		return this.column;
	}
}
