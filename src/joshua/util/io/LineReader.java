package joshua.util.io;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import joshua.decoder.Decoder;

/**
 * This class provides an Iterator interface to a BufferedReader. This covers the most common
 * use-cases for reading from files without ugly code to check whether we got a line or not.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class LineReader implements Reader<String> {

  /*
   * Note: charset name is case-agnostic "UTF-8" is the canonical name "UTF8", "unicode-1-1-utf-8"
   * are aliases Java doesn't distinguish utf8 vs UTF-8 like Perl does
   */
  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");

  /*
   * The reader and its underlying input stream. We need to keep a hold of the underlying
   * input stream so that we can query how many raw bytes it's read (for a generic progress
   * meter that works across GZIP'ed and plain text files).
   */
  private BufferedReader reader;
  private ProgressInputStream rawStream;

  private String buffer;
  private IOException error;

  private int lineno = 0;
  
  private boolean display_progress = false;
  
  private int progress = 0;

  // ===============================================================
  // Constructors and destructors
  // ===============================================================

  /**
   * Opens a file for iterating line by line. The special "-" filename can be used to specify
   * STDIN. GZIP'd files are tested for automatically.
   * 
   * @param filename the file to be opened ("-" for STDIN)
   */
  public LineReader(String filename) throws IOException {
    
    display_progress = (Decoder.VERBOSE >= 1);
    
    progress = 0;
    
    InputStream stream = null; 
    long totalBytes = -1;
    if (filename.equals("-")) {
      rawStream = null;
      stream = new FileInputStream(FileDescriptor.in);
    } else {
      totalBytes = new File(filename).length();
      rawStream = new ProgressInputStream(new FileInputStream(filename), totalBytes);
      
      try {
        stream = new GZIPInputStream(rawStream);
      } catch (ZipException e) {
        // GZIP ate a byte, so reset
        rawStream.close();
        stream = rawStream = new ProgressInputStream(new FileInputStream(filename), totalBytes);
      }
    } 
    
    this.reader = new BufferedReader(new InputStreamReader(stream, FILE_ENCODING));
  }
  
  public LineReader(String filename, boolean show_progress) throws IOException {
    this(filename);
    display_progress = (Decoder.VERBOSE >= 1 && show_progress);
  }


  /**
   * Wraps an InputStream for iterating line by line. Stream encoding is assumed to be UTF-8.
   */
  public LineReader(InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in, FILE_ENCODING));
    display_progress = false;
  }
  
  /**
   * Chain to the underlying {@link ProgressInputStream}. 
   * 
   * @return an integer from 0..100, indicating how much of the file has been read.
   */
  public int progress() {
    return rawStream == null ? 0 : rawStream.progress();
  }
  
  /**
   * This method will close the file handle, and will raise any exceptions that occured during
   * iteration. The method is idempotent, and all calls after the first are no-ops (unless the
   * thread was interrupted or killed). For correctness, you <b>must</b> call this method before the
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
   * We attempt to avoid leaking file descriptors if you fail to call close before the object falls
   * out of scope. However, the language spec makes <b>no guarantees</b> about timeliness of garbage
   * collection. It is a bug to rely on this method to release the resources. Also, the garbage
   * collector will discard any exceptions that have queued up, without notifying the application in
   * any way.
   * 
   * Having a finalizer means the JVM can't do "fast allocation" of LineReader objects (or
   * subclasses). This isn't too important due to disk latency, but may be worth noting.
   * 
   * @see <a
   *      href="http://java2go.blogspot.com/2007/09/javaone-2007-performance-tips-2-finish.html">Performance
   *      Tips</a>
   * @see <a
   *      href="http://www.javaworld.com/javaworld/jw-06-1998/jw-06-techniques.html?page=1">Techniques</a>
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



  // ===============================================================
  // Reader
  // ===============================================================

  // Copied from interface documentation.
  /** Determine if the reader is ready to read a line. */
  public boolean ready() throws IOException {
    return this.reader.ready();
  }


  /**
   * This method is like next() except that it throws the IOException directly. If there are no
   * lines to be read then null is returned.
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


  // ===============================================================
  // Iterable -- because sometimes Java can be very stupid
  // ===============================================================

  /** Return self as an iterator. */
  public Iterator<String> iterator() {
    return this;
  }


  // ===============================================================
  // Iterator
  // ===============================================================

  // Copied from interface documentation.
  /**
   * Returns <code>true</code> if the iteration has more elements. (In other words, returns
   * <code>true</code> if <code>next</code> would return an element rather than throwing an
   * exception.)
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
   * Return the next line of the file. If an error is encountered, NoSuchElementException is thrown.
   * The actual IOException encountered will be thrown later, when the LineReader is closed. Also if
   * there is no line to be read then NoSuchElementException is thrown.
   */
  public String next() throws NoSuchElementException {
    if (this.hasNext()) {
      if (display_progress) {
        int newProgress = (reader != null) ? progress() : 100;
//        System.err.println(String.format("OLD %d NEW %d", progress, newProgress));
        
        if (newProgress > progress) {
          for (int i = progress + 1; i <= newProgress; i++)
            if (i == 97) {
              System.err.print("1");
            } else if (i == 98) {
              System.err.print("0");
            } else if (i == 99) {
              System.err.print("0");
            } else if (i == 100) {
              System.err.println("%");
            } else if (i % 10 == 0) {
              System.err.print(String.format("%d", i));
              System.err.flush();
            } else if ((i - 1) % 10 == 0)
              ; // skip at 11 since 10, 20, etc take two digits
            else {
              System.err.print(".");
              System.err.flush();
            }
          progress = newProgress;
        }
      }
      
      String line = this.buffer;
      this.lineno++;
      this.buffer = null;
      return line;
    } else {
      throw new NoSuchElementException();
    }
  }
  
  /* Get the line number of the last line that was returned */
  public int lineno() {
    return this.lineno;
  }

  /** Unsupported. */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }


  /**
   * Iterates over all lines, ignoring their contents, and returns the count of lines. If some lines
   * have already been read, this will return the count of remaining lines. Because no lines will
   * remain after calling this method, we implicitly call close.
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

  // ===============================================================
  // Main
  // ===============================================================

  /** Example usage code. */
  public static void main(String[] args) {
    if (1 != args.length) {
      System.out.println("Usage: java LineReader filename");
      System.exit(1);
    }

    try {

      LineReader in = new LineReader(args[0]);
      try {
        for (String line : in) {

          System.out.println(line);

        }
      } finally {
        in.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
