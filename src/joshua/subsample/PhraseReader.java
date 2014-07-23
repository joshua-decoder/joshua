/*
 * This file is based on the edu.umd.clip.mt.PhraseReader class from the University of Maryland's
 * umd-hadoop-mt-0.01 project. That project is released under the terms of the Apache License 2.0,
 * but with special permission for the Joshua Machine Translation System to release modifications
 * under the LGPL version 2.1. LGPL version 3 requires no special permission since it is compatible
 * with Apache License 2.0
 */
package joshua.subsample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import joshua.corpus.BasicPhrase;


/**
 * Wrapper class to read in each line as a BasicPhrase.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class PhraseReader extends BufferedReader {
  private byte language;

  public PhraseReader(Reader r, byte language) {
    super(r);
    this.language = language;
  }

  public BasicPhrase readPhrase() throws IOException {
    String line = super.readLine();
    return (line == null ? null : new BasicPhrase(this.language, line));
  }
}
