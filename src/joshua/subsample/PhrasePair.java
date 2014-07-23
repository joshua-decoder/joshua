/*
 * This file is based on the edu.umd.clip.mt.PhrasePair class from the University of Maryland's
 * umd-hadoop-mt-0.01 project. That project is released under the terms of the Apache License 2.0,
 * but with special permission for the Joshua Machine Translation System to release modifications
 * under the LGPL version 2.1. LGPL version 3 requires no special permission since it is compatible
 * with Apache License 2.0
 */
package joshua.subsample;

// TODO: if we generalize the Alignment class, we could move this
// to joshua.util.sentence.

import joshua.corpus.Phrase;


/**
 * Phrase-aligned tuple class associating an F phrase, E phrase, and (possibly null)
 * word-alignments. This is primarily for maintaining sentence-alignment.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class PhrasePair {
  // Making these final requires Java6, not Java5
  private final Phrase f;
  private final Phrase e;
  private final Alignment a;

  // ===============================================================
  // Constructors
  // ===============================================================
  public PhrasePair(Phrase f_, Phrase e_) {
    this(f_, e_, null);
  }

  public PhrasePair(Phrase f, Phrase e, Alignment a) {
    this.f = f;
    this.e = e;
    this.a = a;
  }

  // ===============================================================
  // Attributes
  // ===============================================================
  public Phrase getF() {
    return f;
  }

  public Phrase getE() {
    return e;
  }

  public Alignment getAlignment() {
    return a;
  }

  // ===============================================================
  // Methods
  // ===============================================================
  public float ratioFtoE() {
    return ((float) this.f.size()) / ((float) this.e.size());
  }
}
