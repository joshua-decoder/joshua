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
package joshua.util.io;

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * This class provides an Iterator interface to a BufferedReader.
 * This covers the most common use-cases for reading from files
 * without ugly code to check whether we got a line or not.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class LineReader implements Reader<String> {
	
	/* Note: charset name is case-agnostic
	 * "UTF-8" is the canonical name
	 * "UTF8", "unicode-1-1-utf-8" are aliases
	 * Java doesn't distinguish utf8 vs UTF-8 like Perl does
	 */
	private static final Charset FILE_ENCODING = Charset.forName("UTF-8");
	
	private BufferedReader reader;
	private String         buffer;
	private IOException    error;
	
	
//===============================================================
// Constructors and destructors
//===============================================================
	
	/**
	 * Opens a file for iterating line by line. If the file
	 * name ends in ".gz" then we automatically open it with
	 * GZIP. File encoding is assumed to be UTF-8.
	 *
	 * @param filename the file to be opened
	 */
	public LineReader(String filename) throws IOException {
		this(LineReader.getInputStream(filename));
	}
	
	
	/**
	 * Wraps an InputStream for iterating line by line. Stream
	 * encoding is assumed to be UTF-8.
	 */
	public LineReader(InputStream in) {
		this.reader = new BufferedReader(
			new InputStreamReader(in, FILE_ENCODING));
	}
	
	
	/**
	 * Uses a BufferedReader for iterating line by line.
	 */
	public LineReader(BufferedReader reader) {
		this.reader = reader;
	}
	
	
	/**
	 * Returns an InputStream for a filename, using Joshua's
	 * canonical means for interpreting that name (e.g\ detecting
	 * gzipped files). This is used by the LineReader constructor
	 * that accepts a String argument.
	 *
	 * @deprecated This method is provided in order for
	 * {@link joshua.decoder.DecoderThread} to open files in
	 * the canonical way for handing off to
	 * {@link joshua.decoder.segment_file.SegmentFileParser}.
	 * The <code>SegmentFileParser</code> interface can't be
	 * made more liberal (e.g. to accept a {@link java.io.Reader})
	 * because {@link javax.xml.parsers.SAXParser} can't parse
	 * that argument and no common {@link java.io.Reader} gives
	 * access to the underlying <code>InputStream</code>. This
	 * method is considered a hack which should be removed once
	 * a better solution presents itself.
	 */
	@Deprecated
	public static final InputStream getInputStream(String filename)
	throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		return (filename.endsWith(".gz") ? new GZIPInputStream(fis) : fis);
	}
	
	
	/**
	 * This method will close the file handle, and will raise
	 * any exceptions that occured during iteration. The method
	 * is idempotent, and all calls after the first are no-ops
	 * (unless the thread was interrupted or killed). For
	 * correctness, you <b>must</b> call this method before the
	 * object falls out of scope.
	 */
	public void close() throws IOException {
		
		this.buffer = null; // Just in case it's a large string
		
		if (null != this.reader) {
			try {
				// We assume the wrappers will percolate this down.
				this.reader.close();
				
			} catch (IOException e) {
				// We need to trash our cached error for idempotence.
				// Presumably the closing error is the more important
				// one to throw.
				this.error = null;
				throw e;
				
			} finally {
				this.reader = null;
			}
		}
		
		if (null != this.error) {
			IOException e = this.error;
			this.error = null;
			throw e;
		}
	}
	
	
	/** 
	 * We attempt to avoid leaking file descriptors if you fail
	 * to call close before the object falls out of scope.
	 * However, the language spec makes <b>no guarantees</b>
	 * about timeliness of garbage collection. It is a bug to
	 * rely on this method to release the resources. Also, the
	 * garbage collector will discard any exceptions that have
	 * queued up, without notifying the application in any way.
	 *
	 * Having a finalizer means the JVM can't do "fast allocation"
	 * of LineReader objects (or subclasses). This isn't too
	 * important due to disk latency, but may be worth noting.
	 * 
	 * @see <a href="http://java2go.blogspot.com/2007/09/javaone-2007-performance-tips-2-finish.html">Performance Tips</a>
	 * @see <a href="http://www.javaworld.com/javaworld/jw-06-1998/jw-06-techniques.html?page=1">Techniques</a>
	 */
	protected void finalize() throws Throwable {
		try {
			this.close();
		} catch (IOException e) {
			// Do nothing. The GC will discard the exception
			// anyways, but it may cause us to linger on the heap.
		} finally {
			super.finalize();
		}
	}
	
	
	
//===============================================================
// Reader
//===============================================================
	
	// Copied from interface documentation.
	/** Determine if the reader is ready to read a line. */
	public boolean ready() throws IOException {
		return this.reader.ready();
	}
	
	
	/**
	 * This method is like next() except that it throws the
	 * IOException directly. If there are no lines to be read
	 * then null is returned.
	 */
	public String readLine() throws IOException {
		if (this.hasNext()) {
			String line = this.buffer;
			this.buffer = null;
			return line;
			
		} else {
			if (null != this.error) {
				IOException e = this.error;
				this.error = null;
				throw e;
			}
			return null;
		}
	}
	
	
//===============================================================
// Iterable -- because sometimes Java can be very stupid
//===============================================================
	
	/** Return self as an iterator. */
	public Iterator<String> iterator() {
		return this;
	}
	
	
//===============================================================
// Iterator
//===============================================================
	
	// Copied from interface documentation.
	/**
	 * Returns <code>true</code> if the iteration has more
	 * elements. (In other words, returns <code>true</code> if
	 * <code>next</code> would return an element rather than
	 * throwing an exception.)
	 */
	public boolean hasNext() {
		if (null != this.buffer) {
			return true;
			
		} else if (null != this.error) {
			return false;
			
		} else {
			// We're not allowed to throw IOException from within Iterator
			try {
				this.buffer = this.reader.readLine();
			} catch (IOException e) {
				this.buffer = null;
				this.error = e;
				return false;
			}
			return (null != this.buffer);
		}
	}
	
	
	/**
	 * Return the next line of the file. If an error is
	 * encountered, NoSuchElementException is thrown. The actual
	 * IOException encountered will be thrown later, when the
	 * LineReader is closed. Also if there is no line to be
	 * read then NoSuchElementException is thrown.
	 */
	public String next() throws NoSuchElementException {
		if (this.hasNext()) {
			String line = this.buffer;
			this.buffer = null;
			return line;
		} else {
			throw new NoSuchElementException();
		}
	}
	
	
	/** Unsupported. */
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Iterates over all lines, ignoring their contents, and
	 * returns the count of lines. If some lines have already
	 * been read, this will return the count of remaining lines.
	 * Because no lines will remain after calling this method,
	 * we implicitly call close.
	 * 
	 * @return the number of lines read
	 */
	public int countLines() throws IOException {
		int lines = 0;
		
		while (this.hasNext()) {
			this.next();
			lines++;
		}
		this.close();
		
		return lines;
	}
	
//===============================================================
// Main
//===============================================================
	
	/** Example usage code. */
	public static void main(String[] args) {
		if (1 != args.length) {
			System.out.println("Usage: java LineReader filename");
			System.exit(1);
		}
		
		try {
			
			LineReader in = new LineReader(args[0]);
			try { for (String line : in) {
				
				System.out.println(line);
				
			} } finally { in.close(); }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
