/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.corpus;

// Imports
import edu.jhu.util.sentence.*;
import edu.jhu.util.sentence.phrase_extraction.*;
import edu.jhu.util.suffix_array.*;
import edu.jhu.util.*;
import edu.jhu.util.math.Probability;

import java.util.*;
import java.io.*;
/**
 * SuffixArrayParallelCorpus is a corpus which implements the 
 * ParallelCorpus interface using suffix arrays.
 *
 * @author Chris Callison-Burch
 * @since  13 February 2005@version $LastChangedDate$
 */
public class SuffixArrayParallelCorpus implements ParallelCorpus {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	protected SuffixArray sourceCorpus;
	protected SuffixArray targetCorpus;
	protected Grids alignmentGrids;
	protected PhraseExtractor phraseExtractor;
	
	/** The maximum number of instances of a phrase to consider when
	  * retrieving translations. */
	protected int sampleSize;
 
 
//===============================================================
// Constructor(s)
//===============================================================

	public SuffixArrayParallelCorpus(String corpusName, String sourceLang, String targetLang, 
									 String directory, PhraseExtractor phraseExtractor) throws IOException {
		sourceCorpus = SuffixArrayFactory.loadSuffixArray(sourceLang, corpusName, directory);
		targetCorpus = SuffixArrayFactory.loadSuffixArray(targetLang, corpusName, directory);
		alignmentGrids = SuffixArrayFactory.loadAlignments(sourceLang, targetLang, corpusName, directory, sourceCorpus, targetCorpus);
		this.phraseExtractor = phraseExtractor;
		sampleSize = Integer.MAX_VALUE;
	}
	
	
	public SuffixArrayParallelCorpus(SuffixArray sourceCorpus, SuffixArray targetCorpus,
									 Grids alignmentGrids, PhraseExtractor phraseExtractor) {
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		this.alignmentGrids = alignmentGrids;
		this.phraseExtractor = phraseExtractor;
		sampleSize = Integer.MAX_VALUE;
	}
	
	/**
	 * Mirrors the member variables of the specified parallel corpus.
	 */
	protected SuffixArrayParallelCorpus(SuffixArrayParallelCorpus parallelCorpus) {
		this.sourceCorpus = parallelCorpus.sourceCorpus;
		this.targetCorpus = parallelCorpus.targetCorpus;
		this.alignmentGrids = parallelCorpus.alignmentGrids;
		this.phraseExtractor = parallelCorpus.phraseExtractor;
		this.sampleSize = parallelCorpus.sampleSize;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}
	
	public int getSampleSize() {
		return sampleSize;
	}	
	
	/** 
	 * @return the phrase extractor currently in use by the getTranslations methods.
	 */
	public PhraseExtractor getPhraseExtractor() {
		return phraseExtractor;
	} 
	
	/** 
	 * @return the phrase extractor to use in the getTranslations methods.
	 */
	public void setPhraseExtractor(PhraseExtractor phraseExtractor) {
		this.phraseExtractor = phraseExtractor;
	}
	
	/**
	 * @return the source corpus 
	 */
	public Corpus getSourceCorpus() {
		return sourceCorpus;
	}

	/** 
	 * @return the target corpus 
	 */	
	public Corpus getTargetCorpus() {
		return targetCorpus;
	}
	
	
	
	/**
	 * @returns the number of sentences in the corpus.
	 */
	public int getNumSentences() {
		return sourceCorpus.getNumSentences();
	}
	
	
	/** 
	 * @param sentencePairNumber the index of the sentence pair to retrieve
	 * @return the alignment for the specified sentence pair.
	 */
	public Alignment getSentencePair(int sentencePairNumber) {
		Phrase source = sourceCorpus.getSentence(sentencePairNumber);
		Phrase target = targetCorpus.getSentence(sentencePairNumber);
		Grid grid = alignmentGrids.getGrid(sentencePairNumber);
		return new Alignment(source, target, grid);
	}
	

	//===========================================================
	// Methods
	//===========================================================


	/** Collects all of the translations for the specified source phrase
	  * and returns a collection of the translations (in the form of 
	  * Alignments) associated with their frequency.
	  * @param sourcePhrase the phrase to find translations of
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfSource(Phrase sourcePhrase, TranslationResultsSet resultsSet) {
		getTranslationsOfSource(sourcePhrase, sampleSize, resultsSet);
	}
	
	
	/** Collects up to a specified number of translations of the 
	  * source phrase and returns their frequencies. 
	  * @param sourcePhrase the phrase to find translations of
	  * @param maxInstances the maximum number of translations to collect
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfSource(Phrase sourcePhrase, int maxInstances, TranslationResultsSet resultsSet) {
		// Look up all occurrences of the sourcePhrase in the sourceCorpus
		int[] bounds = sourceCorpus.findPhrase(sourcePhrase);
		// if the phrase is not found, then do nothing...
		if(bounds == null) return;
		int numOccurrences = Math.min(maxInstances, bounds[1]-bounds[0]);
	
		// Find the sentence indexes.
		for(int i = 0; i <= numOccurrences; i++) {		
			int sentenceIndex = sourceCorpus.getSentenceIndex(sourceCorpus.getCorpusIndex(bounds[0]+i));
			int startPosition = sourceCorpus.getCorpusIndex(bounds[0]+i) - 
								sourceCorpus.getSentencePosition(sentenceIndex);
			int endPosition = startPosition + sourcePhrase.size();
			
			Alignment alignment = getSentencePair(sentenceIndex);
			try {
				Collection translations = phraseExtractor.getSourceAlignmentSpans(alignment.getAlignmentPoints(), startPosition, endPosition, 
																					sourceCorpus.getCorpusIndex(bounds[0]+i),
																					sourceCorpus.getCorpusIndex(bounds[0]+i)+sourcePhrase.size());
				Iterator it = translations.iterator();
				while(it.hasNext()) {
					Span span = (Span) it.next();
					Alignment translation = alignment.getSubAlignment(span);
					resultsSet.incrementCount(translation, sentenceIndex, span);
				}
				
			} catch (IndexOutOfBoundsException e) {
				// Sometimes the matching phrase can span sentences.
				// We catch this as an exception rather than checking every
				// instance.  An alternate would be to put sentence markers
				// around each sentence so that we don't find spanning segments
				// unless we look for them explicitly.  
			}
		}
	}



	/** Collects all of the translations for the specified target phrase
	  * and returns a collection of the translations (in the form of 
	  * Alignments) associated with their frequency.
	  * @param targetPhrase the phrase to find translations of
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfTarget(Phrase targetPhrase, TranslationResultsSet resultsSet) {
		getTranslationsOfTarget(targetPhrase, sampleSize, resultsSet);
	}
	
	/** Collects up to a specified number of translations of the 
	  * target phrase and returns their frequencies. 
	  * @param targetPhrase the phrase to find translations of
	  * @param maxInstances the maximum number of translations to collect
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfTarget(Phrase targetPhrase, int maxInstances, TranslationResultsSet resultsSet) {
		// Look up all occurrences of the targetPhrase in the targetCorpus
		int[] bounds = targetCorpus.findPhrase(targetPhrase);
		// if the phrase is not found, then do nothing...
		if(bounds == null) return;
		int numOccurrences = Math.min(maxInstances, bounds[1]-bounds[0]);
	
		// Find the sentence indexes.
		for(int i = 0; i <= numOccurrences; i++) {
			int sentenceIndex = targetCorpus.getSentenceIndex(targetCorpus.getCorpusIndex(bounds[0]+i));
			int startPosition = targetCorpus.getCorpusIndex(bounds[0]+i) - 
						        targetCorpus.getSentencePosition(sentenceIndex);
			int endPosition = startPosition + targetPhrase.size();
			
			Alignment alignment = getSentencePair(sentenceIndex);
			try {
				Collection translations = phraseExtractor.getTargetAlignmentSpans(alignment.getAlignmentPoints(), startPosition, endPosition,
																					targetCorpus.getCorpusIndex(bounds[0]+i),
																					targetCorpus.getCorpusIndex(bounds[0]+i)+targetPhrase.size());
				Iterator it = translations.iterator();
				while(it.hasNext()) {
					Span span = (Span) it.next();
					Alignment translation = alignment.getSubAlignment(span);
					resultsSet.incrementCount(translation, sentenceIndex, span);
				}	
			} catch (IndexOutOfBoundsException e) {
				// Sometimes the matching phrase can span sentences.
				// We catch this as an exception rather than checking every
				// instance.  An alternate would be to put sentence markers
				// around each sentence so that we don't find spanning segments
				// unless we look for them explicitly.  
			}			
		}
	}




//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================



//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	/**
	 * This method demonstrates the use of the SuffixArrayParallelCorpus by retriving the translations
	 * of phrases from a file, or interactively.  A threshold may be specified beneith which we won't 
	 * print translations.
	 */
	public static void main(String[] args) throws IOException, ArithmeticException
	{
		try {
			if(args.length < 5) {
				System.out.println("Usage: java SuffixArrayParalellCorpus corpusName sourceLang targetLang directory phrasesToTranslate|-interactive (threshold)");
				System.exit(0);
			}
			String corpusName = args[0];
			String sourceLang = args[1];
			String targetLang = args[2];
			String directory = args[3];
			String filename = args[4];

			Probability threshold;
			if(args.length > 5) {
				threshold =  new Probability(Double.parseDouble(args[5]));
			} else {
				threshold = new Probability(0.01);
			}
				
			PhraseExtractor phraseExtractor = new ConstrainedConsistent();
			SuffixArrayParallelCorpus parallelCorpus = new SuffixArrayParallelCorpus(corpusName, sourceLang, targetLang, directory, phraseExtractor);			
			parallelCorpus.setSampleSize(100);
			Vocabulary vocab = parallelCorpus.sourceCorpus.getVocabulary();
			
			
			BufferedReader reader;
			if(filename.toLowerCase().equals("-interactive")) {
				reader  = new BufferedReader(new InputStreamReader(System.in));
				while (true) {
					System.out.print("\nEnter phrase: ");
					String input = reader.readLine();
					if (input.equals("exit")) System.exit(0);
					if(!input.trim().equals("")) {
						Phrase phrase = new Phrase(input, vocab);
						TranslationResultsSet resultsSet = new TranslationResultsSet();
						parallelCorpus.getTranslationsOfSource(phrase, resultsSet);
						System.out.println(resultsSet.toString(threshold));
					}
				}
			} else {
				reader = FileUtil.getBufferedReader(filename);
				while(reader.ready()) {
					Phrase phrase = new Phrase(reader.readLine(), vocab);
					TranslationResultsSet resultsSet = new TranslationResultsSet();
					parallelCorpus.getTranslationsOfSource(phrase, resultsSet);
					System.out.println(resultsSet.toString(threshold));
				}
			}
			
		} catch(Exception e) {
			System.out.println("Error in loading the SuffixArrayParallelCorpus: " + e);
		}

	}
}

