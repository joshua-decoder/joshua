/*
 * This file is based on the edu.umd.clip.mt.subsample.BiCorpus class from the University of
 * Maryland's jmtTools project (in conjunction with the umd-hadoop-mt-0.01 project). That project is
 * released under the terms of the Apache License 2.0, but with special permission for the Joshua
 * Machine Translation System to release modifications under the LGPL version 2.1. LGPL version 3
 * requires no special permission since it is compatible with Apache License 2.0
 */
package joshua.subsample;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import joshua.corpus.Phrase;


/**
 * Class for representing a sentence-aligned bi-corpus (with optional word-alignments).
 * <p>
 * In order to avoid memory crashes we no longer extend an ArrayList, which tries to cache the
 * entire file in memory at once. This means we'll re-read through each file (1 +
 * {@link Subsampler#MAX_SENTENCE_LENGTH} / binsize) times where binsize is determined by the
 * <code>subsample(String, float, PhraseWriter, BiCorpusFactory)</code> method.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class BiCorpus implements Iterable<PhrasePair> {
  // Making these final requires Java6, doesn't work in Java5
  protected final String foreignFileName;
  protected final String nativeFileName;
  protected final String alignmentFileName;

  // ===============================================================
  // Constructors
  // ===============================================================
  /**
   * Constructor for unaligned BiCorpus.
   */
  public BiCorpus(String foreignFileName, String nativeFileName) throws IOException {
    this(foreignFileName, nativeFileName, null);
  }


  /**
   * Constructor for word-aligned BiCorpus.
   */
  public BiCorpus(String foreignFileName, String nativeFileName, String alignmentFileName)
      throws IOException, IllegalArgumentException, IndexOutOfBoundsException {
    this.foreignFileName = foreignFileName;
    this.nativeFileName = nativeFileName;
    this.alignmentFileName = alignmentFileName;

    // Check for fileLengthMismatchException
    // Of course, that will be checked for in each iteration
    //
    // We write it this way to avoid warnings from the foreach style loop
    Iterator<PhrasePair> it = iterator();
    while (it.hasNext()) {
      it.next();
    }
  }


  // ===============================================================
  // Methods
  // ===============================================================
  // BUG: We don't close file handles. The other reader classes apparently have finalizers to handle
  // this well enough for our purposes, but we should migrate to using joshua.util.io.LineReader and
  // be sure to close it in the end.

  // We're not allowed to throw exceptions from Iterator/Iterable
  // so we have evil boilerplate to crash the system
  /**
   * Iterate through the files represented by this <code>BiCorpus</code>, returning a
   * {@link PhrasePair} for each pair (or triple) of lines.
   */
  @SuppressWarnings("resource")
  public Iterator<PhrasePair> iterator() {
    PhraseReader closureRF = null;
    PhraseReader closureRE = null;
    BufferedReader closureRA = null;
    try {
      closureRF = new PhraseReader(new FileReader(this.foreignFileName), (byte) 1);
      closureRE = new PhraseReader(new FileReader(this.nativeFileName), (byte) 0);
      closureRA =
          (null == this.alignmentFileName ? null : new BufferedReader(new FileReader(
              this.alignmentFileName)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File not found", e);
    }
    // Making final for closure capturing in the local class definition
    final PhraseReader rf = closureRF;
    final PhraseReader re = closureRE;
    final BufferedReader ra = closureRA;

    return new Iterator<PhrasePair>() { /* Local class definition */
      private Phrase nextForeignPhrase = null;

      public void remove() {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
        if (null == this.nextForeignPhrase) {
          try {
            this.nextForeignPhrase = rf.readPhrase();
          } catch (IOException e) {
            throw new RuntimeException("IOException", e);
          }
        }
        return null != this.nextForeignPhrase;
      }

      public PhrasePair next() {
        if (this.hasNext()) {
          Phrase f = this.nextForeignPhrase;

          Phrase e = null;
          try {
            e = re.readPhrase();
          } catch (IOException ioe) {
            throw new RuntimeException("IOException", ioe);
          }
          if (null == e) {
            fileLengthMismatchException();
            return null; // Needed to make javac happy
          } else {
            if (e.size() != 0 && f.size() != 0) {
              if (null != ra) {
                String line = null;
                try {
                  line = ra.readLine();
                } catch (IOException ioe) {
                  throw new RuntimeException("IOException", ioe);
                }

                if (null == line) {
                  fileLengthMismatchException();
                  return null; // Needed to make javac happy
                } else {
                  Alignment a = new Alignment((short) f.size(), (short) e.size(), line);

                  this.nextForeignPhrase = null;
                  return new PhrasePair(f, e, a);
                }
              } else {
                this.nextForeignPhrase = null;
                return new PhrasePair(f, e);
              }
            } else {
              // Inverted while loop
              this.nextForeignPhrase = null;
              return this.next();
            }
          }
        } else {
          throw new NoSuchElementException();
        }
      }
    }; /* End local class definition */
  } /* end iterator() */


  private static void fileLengthMismatchException() throws RuntimeException {
    throw new RuntimeException("Mismatched file lengths!");
  }
}
