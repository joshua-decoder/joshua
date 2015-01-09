package joshua.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generic progress meter for reading files (compressed or not). Pass it the raw input file stream
 * and it will keep track for you.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class ProgressInputStream extends FilterInputStream {

  private long totalBytes = -1;
  private long bytesRead = 0;
  
  protected ProgressInputStream(InputStream in, long totalBytes) {
    super(in);

    this.totalBytes = totalBytes;
  }
  
  @Override
  public int read() throws IOException {
    int value = super.read();
    bytesRead += 1;
    return value;
  }
  
  @Override
  public int read(byte[] b) throws IOException {
    int value = super.read(b);
    bytesRead += value;
    return value;
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int value = super.read(b, off, len);
    bytesRead += value;
    return value;
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    bytesRead = 0;
  }
  
  @Override
  public long skip(long bytesRead) throws IOException {
    long skip = super.skip(bytesRead);
    bytesRead += skip;
    return skip;
  }
  
  /** 
   * @return progress through the file, as an integer (0..100).
   */
  public int progress() {
    return (int)(100.0 * (float)bytesRead / (float)totalBytes);
  }
}
