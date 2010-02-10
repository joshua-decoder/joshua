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
package joshua.corpus.suffix_array;

import joshua.corpus.Corpus;
import joshua.corpus.CorpusArray;
import joshua.corpus.alignment.AlignmentArray;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.vocab.ExternalizableSymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.io.LineReader;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;


/**
 * SuffixArrayFactory is the class that handles the loading and
 * saving of SuffixArrays and their associated classes.
 * 
 * @author Chris Callison-Burch
 * @since  8 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public class SuffixArrayFactory {

	//===============================================================
	// Constants
	//===============================================================

	public static boolean SHOW_PROGRESS = false;
	private static final Logger logger = Logger.getLogger(SuffixArrayFactory.class.getName());

	//===============================================================
	// Static
	//===============================================================

	/**
	 * Gets the standard filename for a Vocabulary.
	 */
	public static String getVocabularyFileName(String lang, String corpus) {
		return(lang+"_"+corpus+"_vocab.txt"); 
	}


	/**
	 * Gets the standard filename for a CorpusArray.
	 */
	public static String getCorpusArrayFileName(String lang, String corpus) {
		return (lang+"_"+corpus+"_sentences.txt"); 
	}


	/**
	 * Gets the standard filename for a SuffixArray.
	 */
	public static String getSuffixArrayFileName(String lang, String corpus) {
		return (lang+"_"+corpus+"_suffixes.txt"); 
	}


	/**
	 * Gets the standard filename for an AlignmentArray.
	 */
	public static String getAlignmentArrayFileName(String sourceLang, String targetLang, String corpus) {
		return (sourceLang+"_"+targetLang+"_"+corpus+"_alignment_array.txt"); 
	}


	// TODO: fuse createVocabulary and createCorpusArray together to avoid allocating the trivial array.
	public static CorpusArray createCorpusArray(String inputFilename)
	throws IOException {
		Vocabulary vocabulary = new Vocabulary();
		int[] ws = Vocabulary.initializeVocabulary(inputFilename, vocabulary, true);
		return createCorpusArray(inputFilename, vocabulary, ws[0], ws[1]);
	}


	// TODO: fuse count and createCorpusArray together to avoid allocating the trivial array.
	public static CorpusArray createCorpusArray(String inputFilename, ExternalizableSymbolTable vocabulary) throws IOException {
		int[] ws = count(inputFilename);
		return createCorpusArray(inputFilename, vocabulary, ws[0], ws[1]);
	}


	/**
	 * Counts the words and sentences in a plain text file.
	 *
	 * @param inputFilename the plain text file
	 * @return a tuple containing the number of words in the
	 *         corpus and number of sentences in the corpus
	 */
	static int[] count(String inputFilename) throws IOException {

		int numSentences = 0;
		int numWords = 0;

		LineReader lineReader = new LineReader(inputFilename);

		for (String line : lineReader) {
			String[] sentence = line.trim().split("\\s+");
			numWords += sentence.length;
			numSentences++;
			if(SHOW_PROGRESS && numSentences % 10000==0) logger.info(""+numWords);
		}

		int[] numberOfWordsSentences = { numWords, numSentences };
		return numberOfWordsSentences;
	}


	/**
	 * Creates a new CorpusArray from a plain text file, given
	 * a symbol table created from the same file.
	 *
	 * @param numWords     the number of words in the file
	 *                     (returned by createVocabulary)
	 * @param numSentences the number of lines in the file
	 *                     (returned by createVocabulary)
	 */
	public static CorpusArray createCorpusArray(String inputFilename, ExternalizableSymbolTable vocab, int numWords, int numSentences) throws IOException {
		// initialize the arrays.
		int[] corpus = new int[numWords];
		int[] sentenceIndexes = new int[numSentences];

		// instantiate them.
		int wordCounter = 0;
		int sentenceCounter = 0;

		LineReader lineReader = new LineReader(inputFilename);

		for (String phraseString : lineReader) {
			int[] words = vocab.getIDs(phraseString);
//			String[] wordStrings = phraseString.split("\\s+");
//			int[] words = new int[wordStrings.length];
//			for (int i = 0; i < wordStrings.length; i++) {
//				words[i] = vocab.getID(wordStrings[i]);
//			}
//
//			BasicPhrase sentence = new BasicPhrase(words, vocab);
			sentenceIndexes[sentenceCounter] = wordCounter;
			sentenceCounter++;
			System.arraycopy(words, 0, corpus, wordCounter, words.length);
			wordCounter += words.length;
//
//			for(int i = 0; i < sentence.size(); i++) {
//				corpus[wordCounter] = sentence.getWordID(i);
//				wordCounter++;
//			}
//			if(SHOW_PROGRESS && sentenceCounter % 10000==0) logger.info(""+numWords);
		}

		return new CorpusArray(corpus, sentenceIndexes, vocab);
	}


	/**
	 * Creates a new SuffixArray from a CorpusArray created
	 * from the same file.
	 */
	public static SuffixArray createSuffixArray(Corpus corpusArray, int maxCacheSize) throws IOException {
		return new SuffixArray(corpusArray, maxCacheSize);
	}


	/**
	 * Creates an Alignments object from a file containing
	 * Moses-style alignments, and a source and target corpus.
	 */
	public static Alignments createAlignments(String alignmentsFilename, Suffixes sourceCorpus, Suffixes targetCorpus) throws IOException {

		// Maps indices from the target corpus to a list of their aligned source points
		Map<Integer,List<Integer>> alignedSourceList = new HashMap<Integer,List<Integer>>();

		// Maps indices from the source corpus to a list of their aligned target points
		Map<Integer,List<Integer>> alignedTargetList = new HashMap<Integer,List<Integer>>();

		// Maps indices from the target corpus to an array of their aligned source points
		int[][] alignedSourceIndices = new int[targetCorpus.size()][];

		// Maps indices from the source corpus to an array of their aligned target points
		int[][] alignedTargetIndices = new int[sourceCorpus.size()][];

		// set the values of the arrays based on the alignments file...
		int sentenceCounter = 0;

		LineReader lineReader = new LineReader(alignmentsFilename);

		for (String line : lineReader) {

			// Start index of current source sentence
			int sourceOffset = sourceCorpus.getSentencePosition(sentenceCounter);

			// Start index of current target sentence
			int targetOffset = targetCorpus.getSentencePosition(sentenceCounter);

			// To save memory, clear old items that will not be used again
			alignedSourceList.clear();
			alignedTargetList.clear();

			// parse the alignment points
			String[] alignments = line.split("\\s+");
			for(int i = 0; i < alignments.length; i++) {
				String[] points = alignments[i].split("-");
				int sourceIndex = sourceOffset + Integer.parseInt(points[0]);
				int targetIndex = targetOffset + Integer.parseInt(points[1]);

				if (!alignedSourceList.containsKey(targetIndex)) {
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(sourceIndex);
					alignedSourceList.put(targetIndex, list);
				} else {
					alignedSourceList.get(targetIndex).add(sourceIndex);
				}

				if (!alignedTargetList.containsKey(sourceIndex)) {
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(targetIndex);
					alignedTargetList.put(sourceIndex, list);
				} else {
					alignedTargetList.get(sourceIndex).add(targetIndex);
				}

			}

			int nextSourceOffset = sourceCorpus.getSentencePosition(sentenceCounter+1);
			int nextTargetOffset = targetCorpus.getSentencePosition(sentenceCounter+1);

			for (int i=targetOffset; i<nextTargetOffset; i++) {

				if (alignedSourceList.containsKey(i)) {
					// List of source points aligned to the target index i
					List<Integer> sourceList = alignedSourceList.get(i);
					Collections.sort(sourceList);
					int size=sourceList.size();
					alignedSourceIndices[i] = new int[size];
					for (int j=0; j<size; j++) alignedSourceIndices[i][j] = sourceList.get(j);
				} else {
					alignedSourceIndices[i] = null;
				}

			}

			for (int i=sourceOffset; i<nextSourceOffset; i++) {

				if (alignedTargetList.containsKey(i)) {
					// List of target points aligned to the source index i
					List<Integer> targetList = alignedTargetList.get(i);
					Collections.sort(alignedTargetList.get(i));
					int size=targetList.size();
					alignedTargetIndices[i] = new int[size];
					for (int j=0; j<size; j++) alignedTargetIndices[i][j] = targetList.get(j);
				} else {
					alignedTargetIndices[i] = null;
				}
			}

			sentenceCounter++;
		}

		return new AlignmentArray(alignedTargetIndices, alignedSourceIndices, sentenceCounter);
	}



	//===============================================================
	// Private 
	//===============================================================

}

