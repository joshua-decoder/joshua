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
/*
 * This file is based on the edu.umd.clip.mt.subsample.BiCorpus
 * class from the University of Maryland's jmtTools project (in
 * conjunction with the umd-hadoop-mt-0.01 project). That project
 * is released under the terms of the Apache License 2.0, but with
 * special permission for the Joshua Machine Translation System to
 * release modifications under the LGPL version 2.1. LGPL version
 * 3 requires no special permission since it is compatible with
 * Apache License 2.0
 */
package joshua.subsample;

import joshua.corpus.Phrase;
import joshua.corpus.vocab.Vocabulary;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;


/**
 * Class for representing a sentence-aligned bi-corpus (with optional
 * word-alignments).
 * <p>
 * In order to avoid memory crashes we no longer extend an ArrayList,
 * which tries to cache the entire file in memory at once. This
 * means we'll re-read through each file (1 +
 * {@link Subsampler#MAX_SENTENCE_LENGTH} / binsize) times where
 * binsize is determined by the
 * <code>subsample(String, float, PhraseWriter, BiCorpusFactory)</code>
 * method.
 *
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class BiCorpus
implements Iterable<PhrasePair> {
	// Making these final requires Java6, doesn't work in Java5
	protected final String     foreignFileName;
	protected final String     nativeFileName;
	protected final String     alignmentFileName;
	protected final Vocabulary foreignVocab;
	protected final Vocabulary nativeVocab;
	
//===============================================================
// Constructors
//===============================================================
	/**
	 * Constructor for unaligned BiCorpus.
	 */
	public BiCorpus(String foreignFileName, String nativeFileName,
		Vocabulary foreignVocab, Vocabulary nativeVocab)
	throws IOException {
		this(foreignFileName, nativeFileName, null, foreignVocab, nativeVocab);
	}
	
	
	/**
	 * Constructor for word-aligned BiCorpus.
	 */
	public BiCorpus(String foreignFileName, String nativeFileName,
		String alignmentFileName,
		Vocabulary foreignVocab, Vocabulary nativeVocab)
	throws IOException, IllegalArgumentException, IndexOutOfBoundsException {
		this.foreignFileName   = foreignFileName;
		this.nativeFileName    = nativeFileName;
		this.alignmentFileName = alignmentFileName;
		this.foreignVocab      = foreignVocab;
		this.nativeVocab       = nativeVocab;
		
		// Check for fileLengthMismatchException
		// Of course, that will be checked for in each iteration
		//
		// We write it this way to avoid warnings from the foreach style loop
		Iterator<PhrasePair> it = iterator();
		while (it.hasNext()) {
			it.next();
		}
	}
	
	
//===============================================================
// Methods
//===============================================================
	// BUG: We don't close file handles. The other reader classes apparently have finalizers to handle this well enough for our purposes, but we should migrate to using joshua.util.io.LineReader and be sure to close it in the end.
	
	// We're not allowed to throw exceptions from Iterator/Iterable
	// so we have evil boilerplate to crash the system
	/**
	 * Iterate through the files represented by this
	 * <code>BiCorpus</code>, returning a {@link PhrasePair}
	 * for each pair (or triple) of lines.
	 */
	public Iterator<PhrasePair> iterator() {
		PhraseReader   closureRF = null;
		PhraseReader   closureRE = null;
		BufferedReader closureRA = null;
		try {
			closureRF = new PhraseReader(
				new FileReader(this.foreignFileName),
				this.foreignVocab,
				(byte)1);
			closureRE = new PhraseReader(
				new FileReader(this.nativeFileName),
				this.nativeVocab,
				(byte)0);
			closureRA = ( null == this.alignmentFileName
				? null
				: new BufferedReader(new FileReader(this.alignmentFileName))
				);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File not found", e);
		}
		// Making final for closure capturing in the local class definition
		final PhraseReader   rf = closureRF;
		final PhraseReader   re = closureRE;
		final BufferedReader ra = closureRA;
		
		
		return new Iterator<PhrasePair>() { /* Local class definition */
			private Phrase nextForeignPhrase = null;
			
			public void remove() { throw new UnsupportedOperationException(); }
			
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
									Alignment a = new Alignment(
										(short)f.size(), (short)e.size(), line);
									
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
	
	
	private static void fileLengthMismatchException()
	throws RuntimeException {
		throw new RuntimeException("Mismatched file lengths!");
	}
}
