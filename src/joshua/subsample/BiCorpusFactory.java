package joshua.subsample;

import java.io.File;
import java.io.IOException;


/**
 * A callback closure for <code>Subsampler.subsample</code>. This class is used by
 * {@link AlignedSubsampler} in order to "override" methods of {@link Subsampler}, minimizing code
 * duplication.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class BiCorpusFactory {
  // Making these final requires Java6, doesn't work in Java5
  protected final String fpath;
  protected final String epath;
  protected final String apath;
  protected final String extf;
  protected final String exte;
  protected final String exta;

  public BiCorpusFactory(String fpath, String epath, String apath, String extf, String exte,
      String exta) {
    // The various concatenation has been moved up here
    // to get it out of the loops where fromFiles is called.
    this.fpath = (fpath == null ? "." : fpath) + File.separator;
    this.epath = (epath == null ? "." : epath) + File.separator;
    this.apath = (apath == null ? "." : apath) + File.separator;
    this.extf = "." + extf;
    this.exte = "." + exte;
    this.exta = (exta == null ? null : "." + exta);
  }


  /** Generate unaligned BiCorpus by default. */
  public BiCorpus fromFiles(String f) throws IOException {
    return this.unalignedFromFiles(f);
  }

  /** Generate unaligned BiCorpus. */
  public BiCorpus unalignedFromFiles(String f) throws IOException {
    return new BiCorpus(fpath + f + extf, epath + f + exte);
  }

  /** Generate aligned BiCorpus. */
  public BiCorpus alignedFromFiles(String f) throws IOException {
    return new BiCorpus(fpath + f + extf, epath + f + exte, apath + f + exta);
  }
}
