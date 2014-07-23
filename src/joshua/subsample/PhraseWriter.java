package joshua.subsample;

import java.io.BufferedWriter;
import java.io.IOException;


/**
 * A PhrasePair-parallel BufferedWriter. In an ideal world we could get the compiler to inline all
 * of this, to have zero-overhead while not duplicating code. Alas, Java's not that cool. The
 * "final" could help on JIT at least.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
final public class PhraseWriter {
  // Making these final requires Java6, not Java5
  private final BufferedWriter wf;
  private final BufferedWriter we;
  private final BufferedWriter wa;

  // ===============================================================
  // Constructors
  // ===============================================================
  public PhraseWriter(BufferedWriter wf_, BufferedWriter we_) {
    this(wf_, we_, null);
  }

  public PhraseWriter(BufferedWriter wf, BufferedWriter we, BufferedWriter wa) {
    this.wf = wf;
    this.we = we;
    this.wa = wa;
  }


  // ===============================================================
  // Methods
  // ===============================================================
  public void write(PhrasePair pp) throws IOException {
    this.wf.write(pp.getF().toString());
    this.we.write(pp.getE().toString());
    if (null != this.wa) this.wa.write(pp.getAlignment().toString());
  }

  public void newLine() throws IOException {
    this.wf.newLine();
    this.we.newLine();
    if (null != this.wa) this.wa.newLine();
  }

  public void flush() throws IOException {
    this.wf.flush();
    this.we.flush();
    if (null != this.wa) this.wa.flush();
  }

  public void close() throws IOException {
    this.wf.close();
    this.we.close();
    if (null != this.wa) this.wa.close();
  }
}
