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

import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.Phrase;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.lang.Iterable;
import java.util.NoSuchElementException;
import java.io.FileNotFoundException;


/**
 * Class for representing a sentence-aligned bi-corpus (with optional
 * word-alignments).
 *
 * In order to not have memory crashes we no longer extend an
 * ArrayList which tries to cache the entire file in memory at once.
 * This means we'll re-read through each file (1 +
 * Subsampler.MAX_SENTENCE_LENGTH / binsize) times where binsize
 * is determined by the subsample(String, float, PhraseWriter,
 * BiCorpusFactor) method.
 *
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton
 */
public class BiCorpus
implements Iterable<PhrasePair> {
	// Can't make these final because Java only believes in
	// assign-at-declaration, not assign-once
	protected String     foreignFileName;
	protected String     englishFileName;
	protected String     alignmentFileName;
	protected Vocabulary foreignVocab;
	protected Vocabulary englishVocab;
	
	
	/**
	 * Constructor for unaligned BiCorpus.
	 */
	public BiCorpus(String ffn, String efn, Vocabulary vf, Vocabulary ve)
	throws IOException {
		this.foreignFileName   = ffn;
		this.englishFileName   = efn;
		this.alignmentFileName = null;
		this.foreignVocab      = vf;
		this.englishVocab      = ve;
		
		// Check for fileLengthMismatchException
		// Of course, that will be checked for in each iteration
		for (PhrasePair pp : this) continue;
	}
	
	
	/**
	 * Constructor for word-aligned BiCorpus.
	 */
	public BiCorpus(String ffn, String efn, String afn, Vocabulary vf, Vocabulary ve)
	throws IOException, IllegalArgumentException, IndexOutOfBoundsException {
		this.foreignFileName   = ffn;
		this.englishFileName   = efn;
		this.alignmentFileName = afn;
		this.foreignVocab      = vf;
		this.englishVocab      = ve;
		
		// Check for fileLengthMismatchException
		// Of course, that will be checked for in each iteration
		for (PhrasePair pp : this) continue;
	}
	
	// We're not allowed to throw exceptions from Iterator/Iterable
	// so we have evil boilerplate to crash the system
	public Iterator<PhrasePair> iterator() {
		PhraseReader   javaSucksRF = null;
		PhraseReader   javaSucksRE = null;
		BufferedReader javaSucksRA = null;
		try {
			javaSucksRF = new PhraseReader(
				new FileReader(this.foreignFileName),
				this.foreignVocab,
				(byte)1);
			javaSucksRE = new PhraseReader(
				new FileReader(this.englishFileName),
				this.englishVocab,
				(byte)0);
			javaSucksRA = ( null == this.alignmentFileName
				? null
				: new BufferedReader(new FileReader(this.alignmentFileName))
				);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		// Making final for closure capturing in the local class definition
		final PhraseReader   rf = javaSucksRF;
		final PhraseReader   re = javaSucksRE;
		final BufferedReader ra = javaSucksRA;
		
		
		return new Iterator<PhrasePair>() { /* Local class definition */
			private Phrase nextForeignPhrase = null;
			
			public void remove() { throw new UnsupportedOperationException(); }
			
			public boolean hasNext() {
				if (null == this.nextForeignPhrase) {
					try {
						this.nextForeignPhrase = rf.readPhrase();
					} catch (IOException ex) {
						ex.printStackTrace();
						System.exit(1);
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
					} catch (IOException ex) {
						ex.printStackTrace();
						System.exit(1);
					}
					if (null == e) fileLengthMismatchException();
					
					if (e.size() != 0 && f.size() != 0) {
						if (null != ra) {
							String line = null;
							try {
								line = ra.readLine();
							} catch (IOException ex) {
								ex.printStackTrace();
								System.exit(1);
							}
							
							if (null == line) fileLengthMismatchException();
							Alignment a = new Alignment(
								(short)f.size(), (short)e.size(), line);
							
							
							this.nextForeignPhrase = null;
							return new PhrasePair(f, e, a);
						} else {
							this.nextForeignPhrase = null;
							return new PhrasePair(f, e);
						}
					} else {
						// Inverted while loop
						this.nextForeignPhrase = null;
						return this.next();
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
