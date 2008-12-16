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
package joshua.sarray;

import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.alignment.Alignments;

import java.util.*;
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
	
	
	/**
	 * Creates a new Vocabulary from a plain text file.
	 *
	 * @param inputFilename the plain text file
	 * @param vocab the Vocabulary to instantiate 
	 * @return a tuple containing the number of words in the corpus and number of sentences in the corpus
	 */
	public static int[] createVocabulary(String inputFilename, Vocabulary vocab) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(inputFilename);
		//HashSet words = new HashSet();
		int numSentences = 0;
		int numWords = 0;
		while (reader.ready()) {
			BasicPhrase sentence = new BasicPhrase(reader.readLine(), vocab);
			numWords += sentence.size();
			numSentences++;
			if(SHOW_PROGRESS && numSentences % 10000==0) System.out.println(numWords);
		}
		reader.close();
		
		vocab.fixVocabulary();
		vocab.alphabetize();
		int[] numberOfWordsSentences = { numWords, numSentences };
		return numberOfWordsSentences;
	}
	
	
	/**
	 * Creates a new CorpusArray from a plain text file, given
	 * a Vocabulary created from the same file.
	 *
	 * @param numWords     the number of words in the file
	 *                     (returned by createVocabulary)
	 * @param numSentences the number of lines in the file
	 *                     (returned by createVocabulary)
	 */
	public static CorpusArray createCorpusArray(String inputFilename, Vocabulary vocab, int numWords, int numSentences) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(inputFilename);
		// initialize the arrays.
		int[] corpus = new int[numWords];
		int[] sentenceIndexes = new int[numSentences];
		
		// instantiate them.
		int wordCounter = 0;
		int sentenceCounter = 0;
		while (reader.ready()) {
			BasicPhrase sentence = new BasicPhrase(reader.readLine(), vocab);
			sentenceIndexes[sentenceCounter] = wordCounter;
			sentenceCounter++;
			
			for(int i = 0; i < sentence.size(); i++) {
				corpus[wordCounter] = sentence.getWordID(i);
				wordCounter++;
			}
			if(SHOW_PROGRESS && sentenceCounter % 10000==0) System.out.println(numWords);
		}
		reader.close();
		return new CorpusArray(corpus, sentenceIndexes, vocab);
	}
	
	
	/**
	 * Creates a new SuffixArray from a CorpusArray created
	 * from the same file.
	 */
	public static SuffixArray createSuffixArray(CorpusArray corpusArray) throws IOException {
		return new SuffixArray(corpusArray);
	}
	
	
	/**
	 * Creates an AlignmentArray from a file containing Moses-style
	 * alignments, and a source and target corpus.
	 */
	public static Alignments createAlignmentArray(String alignmentsFilename, SuffixArray sourceCorpus, SuffixArray targetCorpus) throws IOException {
//		int [] lowestAlignedTargetIndex = initalizeArray(sourceCorpus.size(), AlignmentArray.UNALIGNED);
//		int [] highestAlignedTargetIndex = initalizeArray(sourceCorpus.size(), -1);
//		int [] lowestAlignedSourceIndex = initalizeArray(targetCorpus.size(), AlignmentArray.UNALIGNED);
//		int [] highestAlignedSourceIndex = initalizeArray(targetCorpus.size(), -1);

//		List<List<Integer>> alignedSourceList = new ArrayList<List<Integer>>(targetCorpus.size());
//		for (int i=0; i<targetCorpus.size(); i++) alignedSourceList.add(new ArrayList<Integer>());
//		
//		List<List<Integer>> alignedTargetList = new ArrayList<List<Integer>>(sourceCorpus.size());
//		for (int i=0; i<sourceCorpus.size(); i++) alignedTargetList.add(new ArrayList<Integer>());
		
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
		BufferedReader reader = FileUtil.getBufferedReader(alignmentsFilename);
		while (reader.ready()) {
			
			// Start index of current source sentence
			int sourceOffset = sourceCorpus.getSentencePosition(sentenceCounter);
			
			// Start index of current target sentence
			int targetOffset = targetCorpus.getSentencePosition(sentenceCounter);
			
			// To save memory, clear old items that will not be used again
			alignedSourceList.clear();
			alignedTargetList.clear();
			
			// parse the alignment points
			String[] alignments = reader.readLine().split("\\s+");
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
				
				// set the loweset aligned and highest aligned points
//				lowestAlignedTargetIndex[sourceIndex]  = Math.min(lowestAlignedTargetIndex[sourceIndex], targetIndex);
//				highestAlignedTargetIndex[sourceIndex] = Math.max(highestAlignedTargetIndex[sourceIndex], targetIndex);
//				lowestAlignedSourceIndex[targetIndex]  = Math.min(lowestAlignedSourceIndex[targetIndex], sourceIndex);
//				highestAlignedSourceIndex[targetIndex] = Math.max(highestAlignedSourceIndex[targetIndex], sourceIndex);
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
		reader.close();
		
//		for (int i=0; i<targetCorpus.size(); i++) {
//			Collections.sort(alignedSourceList.get(i));
//			List<Integer> sourceList = alignedSourceList.get(i);
//			int size=sourceList.size();
//			alignedSourceIndices[i] = new int[size];
//			for (int j=0; j<size; j++) alignedSourceIndices[i][j] = sourceList.get(j);
//		}
//		
//		for (int i=0; i<sourceCorpus.size(); i++) {
//			Collections.sort(alignedTargetList.get(i));
//			List<Integer> targetList = alignedTargetList.get(i);
//			int size=targetList.size();
//			alignedTargetIndices[i] = new int[size];
//			for (int j=0; j<size; j++) alignedTargetIndices[i][j] = targetList.get(j);
//		}
		
		//return new AlignmentArray(lowestAlignedTargetIndex, highestAlignedTargetIndex, lowestAlignedSourceIndex, highestAlignedSourceIndex, alignedTargetIndices, alignedSourceIndices);
		return new AlignmentArray(alignedTargetIndices, alignedSourceIndices);
	}
	
	
	/**
	 * Writes the Vocabulary to a file. The file starts with a
	 * line which indicates the number of words, and then
	 * contains an alphabetically sorted list of words, one per
	 * line.
	 */
	public static void saveVocabulary(Vocabulary vocab, String lang, String corpus, String directory) throws IOException {
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getVocabularyFileName(lang, corpus));

		int vocabSize = vocab.size();
		writer.write(Integer.toString(vocabSize));
		writer.newLine();
		for(int i = 0; i < vocabSize; i++) {
			writer.write(vocab.getWord(i));
			writer.newLine();
		}
		writer.close();
		if(SHOW_PROGRESS) System.out.println("Vocabulary saved!");
	}
	
	
	/**
	 * Writes out the corpus in integer representation. The
	 * first line of the file indicates the number of words and
	 * the number of sentences.
	 */
	public static void saveCorpusArray(CorpusArray corpusArray, String lang, String corpus, String directory) throws IOException {
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getCorpusArrayFileName(lang, corpus));
		
		//write out size data on first line
		writer.write(Integer.toString(corpusArray.size()));
		writer.write(" ");
		writer.write(Integer.toString(corpusArray.getNumSentences()));
		int sentenceNum = 0;
		for(int i = 0; i<corpusArray.size(); i++) {
			if (sentenceNum<corpusArray.getNumSentences() && corpusArray.sentences[sentenceNum]==i) {
				sentenceNum++;
				writer.newLine();
			} else {
				writer.write(" ");
			}
			writer.write(Integer.toString(corpusArray.corpus[i]));
		}
		writer.newLine();
		writer.close();
	}
	
	
	/**
	 * Writes the suffix array to file. The first line of the
	 * file indicates the number of words and the number of
	 * sentences.
	 */
	public static void saveSuffixArray(SuffixArray suffixArray, String lang, String corpus, String directory) throws IOException {
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getSuffixArrayFileName(lang, corpus));
		
		//write out size data on first line
		int numSuffixes = suffixArray.size();
		writer.write(Integer.toString(numSuffixes));
		writer.newLine();
		//int suffixNum = 0;
		for(int i = 0; i < numSuffixes; i++) {
			writer.write(Integer.toString(suffixArray.suffixes[i]));
			writer.newLine();
		}
		writer.close();
	}
	
	
	/**
	 * Writes an alignment array to file. The first line of the
	 * file contains two numbers: The number of lines in the
	 * alignedTargetIndex arrays and the alignedSourceIndex
	 * arrays (which correspond to the number of words in the
	 * source and the target corpus, respectively).
	 */
	//TODO Either update this method or delete it
//	public static void saveAlignmentArray(AlignmentArray alignmentArray, String sourceLang, String targetLang, String corpus, String directory) throws IOException {
//		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getAlignmentArrayFileName(sourceLang, targetLang, corpus));
//
//		// write the header line
//		int sourceCorpusLength = alignmentArray.lowestAlignedTargetIndex.length;
//		int targetCorpusLength = alignmentArray.lowestAlignedSourceIndex.length;
//		writer.write(Integer.toString(sourceCorpusLength) + " " + Integer.toString(targetCorpusLength));
//		writer.newLine();
//		
//		// write the source corpus arrays
//		for(int i = 0; i < sourceCorpusLength; i++) {
//			writer.write(Integer.toString(alignmentArray.lowestAlignedTargetIndex[i]) + " " + Integer.toString(alignmentArray.highestAlignedTargetIndex[i]));
//			writer.newLine();
//		}
//		// write the target corpus arrays
//		for(int i = 0; i < targetCorpusLength; i++) {
//			writer.write(Integer.toString(alignmentArray.lowestAlignedSourceIndex[i]) + " " + Integer.toString(alignmentArray.highestAlignedSourceIndex[i]));
//			writer.newLine();
//		}
//		writer.close();
//	}
	


	/**
	 * Loads a Vocabulary from a file containing an alphabetized
	 * list of words, one on each line. The first of the file
	 * line contains the number of words.
	 */
	public static Vocabulary loadVocabulary(String lang, String corpus, String directory) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(directory, getVocabularyFileName(lang, corpus));
		//int vocabSize = 
			Integer.parseInt(reader.readLine());
		Vocabulary vocab = new Vocabulary();
		
		int i = 0;
		while(reader.ready()) {
			vocab.addWord(reader.readLine());
			if(SHOW_PROGRESS && i % 10000==0) System.out.println(i);
			i++;
		}
		reader.close();
		vocab.fixVocabulary();
		return vocab;
	}
	
	
	 /**
	  * Reads in a corpus that is already in integer representation.
	  * Assumes that the first line indicates the number of words
	  * and the number of sentences, as ouput by saveCorpusArray.
	  */
	public static CorpusArray loadCorpusArray(String lang, String corpusName, String directory) throws IOException {
		Vocabulary vocab = loadVocabulary(lang, corpusName, directory);
		BufferedReader reader = FileUtil.getBufferedReader(directory, getCorpusArrayFileName(lang, corpusName));
		 
		//read words and sentences from first line
		StringTokenizer st = new StringTokenizer(reader.readLine());
		int numWords = Integer.parseInt(st.nextToken());
		int numSentences = Integer.parseInt(st.nextToken());
		 
		//initialize
		int[] corpus = new int[numWords];
		int[] sentences = new int[numSentences];
		int wordCounter = 0;
		int sentenceCounter = 0;
		 
		//loop through file
		while(reader.ready()) {
			sentences[sentenceCounter++] = wordCounter;
			String[] wordInts = reader.readLine().split("\\s+");
			for(int i = 0; i < wordInts.length; i++) {
				corpus[wordCounter++] = Integer.parseInt(wordInts[i]);
			}
		}
		reader.close();
		return new CorpusArray(corpus, sentences, vocab);
	}


	 /**
	  * Reads a sorted suffix array from a file. The first line
	  * of the file indicates the number of suffixes.
	  */
	 public static SuffixArray loadSuffixArray(String lang, String corpusName, String directory) throws IOException {
		CorpusArray corpusArray = loadCorpusArray(lang, corpusName, directory);
		BufferedReader reader = FileUtil.getBufferedReader(directory, getSuffixArrayFileName(lang, corpusName));

		//read in size data from the first line
		int numSuffixes = Integer.parseInt(reader.readLine());
		int[] suffixes = new int[numSuffixes];
		for(int i = 0; i < numSuffixes; i++) {
			suffixes[i] = Integer.parseInt(reader.readLine());
		}
		reader.close();
		
		return new SuffixArray(suffixes, corpusArray);
	 }


	/**
	 * Reads an allignment array from a file.  The first line
	 * of the file indicates the number of elements in the
	 * alignedTargetIndex arrays and the alignedSourceIndex arrays.
	 */
/*	public static AlignmentArray loadAlignmentArray(String sourceLang, String targetLang, String corpus, String directory) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(directory, getAlignmentArrayFileName(sourceLang, targetLang, corpus));
		// read the header line...
		String[] fields = reader.readLine().split("\\s+");
		int sourceCorpusLength = Integer.parseInt(fields[0]);
		int targetCorpusLength = Integer.parseInt(fields[1]);
		
		int[] lowestAlignedTargetIndex = new int[sourceCorpusLength];
		int[] highestAlignedTargetIndex = new int[sourceCorpusLength];
		int[] lowestAlignedSourceIndex = new int[targetCorpusLength];
		int[] highestAlignedSourceIndex = new int[targetCorpusLength];

		for(int i = 0; i < sourceCorpusLength; i++) {
			fields = reader.readLine().split("\\s+");
			lowestAlignedTargetIndex[i] = Integer.parseInt(fields[0]);
			highestAlignedTargetIndex[i] = Integer.parseInt(fields[1]);
		}
		
		for(int i = 0; i < targetCorpusLength; i++) {
			fields = reader.readLine().split("\\s+");
			lowestAlignedSourceIndex[i] = Integer.parseInt(fields[0]);
			highestAlignedSourceIndex[i] = Integer.parseInt(fields[1]);
		}
	
		reader.close();
		return new AlignmentArray(lowestAlignedTargetIndex, highestAlignedTargetIndex, lowestAlignedSourceIndex, highestAlignedSourceIndex);
	}
*/	
	

//===============================================================
// Private 
//===============================================================

	
	/**
	 * Creates an int array of the specified length where every
	 * cell is set to the specified initialValue.
	 */
	 //TODO This method is not used.
//	private static int[] initalizeArray(int length, int initialValue) {
//		int[] array = new int[length];
//		for(int i = 0; i < length; i++) {
//			array[i] = initialValue;
//		}
//		return array;
//	}
	
//===============================================================
// Main 
//===============================================================
/*
	public static void main(String[] args) throws java.io.IOException {
		if(args.length != 7) {
			System.out.println("This program indexes a parallel corpus.");
			System.out.println("Usage: java SuffixArrayFactory sourceFile targetFile alignments sourceLang targetLang corpusName outputDir");
			System.exit(0);
		}
		
		String sourceFilename = args[0];
		String targetFilename = args[1];
		String alignmentFilename = args[2];
		String sourceLang = args[3];
		String targetLang = args[4];
		String corpusName = args[5];
		String outputDirectory = args[6];
		
		// Create the vocab, corpus, and suffix arrays for the source corpus
		Vocabulary sourceVocab = new Vocabulary();
		int[] numberOfSourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFilename, sourceVocab);
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFilename, sourceVocab, numberOfSourceWordsSentences[0], numberOfSourceWordsSentences[1]);
		SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);
		
		SuffixArrayFactory.saveVocabulary(sourceVocab, sourceLang, corpusName, outputDirectory);
		SuffixArrayFactory.saveCorpusArray(sourceCorpusArray, sourceLang, corpusName, outputDirectory);
		SuffixArrayFactory.saveSuffixArray(sourceSuffixArray, sourceLang, corpusName, outputDirectory);


		// Create the vocab, corpus, and suffix arrays for the target corpus
		Vocabulary targetVocab = new Vocabulary();
		int[] numberOfTargetWordsSentences = SuffixArrayFactory.createVocabulary(targetFilename, targetVocab);
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFilename, targetVocab, numberOfTargetWordsSentences[0], numberOfTargetWordsSentences[1]);
		SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);

		SuffixArrayFactory.saveVocabulary(targetVocab, targetLang, corpusName, outputDirectory);
		SuffixArrayFactory.saveCorpusArray(targetCorpusArray, targetLang, corpusName, outputDirectory);
		SuffixArrayFactory.saveSuffixArray(targetSuffixArray, targetLang, corpusName, outputDirectory);

		
		// Create the AlignmentArray 
		AlignmentArray alignmentArray = SuffixArrayFactory.createAlignmentArray(alignmentFilename, sourceSuffixArray, targetSuffixArray);
		SuffixArrayFactory.saveAlignmentArray(alignmentArray, sourceLang, targetLang, corpusName, outputDirectory);
		
	}
*/
/*	
	public static void main2(String[] args) throws java.io.IOException {
		if(args.length != 4) {
			System.out.println("Usage: java SuffixArrayFactory file corpusName lang outputDir");
			System.exit(0);
		}
		
		String inputFilename = args[0];
		String corpusName = args[1];
		String lang = args[2];
		String outputDirectory = args[3];
		
		// Create the vocab, corpus, and suffix arrays
		Vocabulary vocab = new Vocabulary();
		int[] numberOfWordsSentences = SuffixArrayFactory.createVocabulary(inputFilename, vocab);
		
		// Check to see if a vocabulary file already exists in the outputDir, if so use it 
		// ccb - todo - might want to check that the existing vocabulary file is a superset of this one. 
		if(FileUtil.exists(outputDirectory, getVocabularyFileName(lang, corpusName))) {
			System.out.println("Using existing vocabulary file at " + getVocabularyFileName(lang, corpusName));
			vocab = loadVocabulary(lang, corpusName, outputDirectory);
		}
		
		CorpusArray corpusArray = SuffixArrayFactory.createCorpusArray(inputFilename, vocab, numberOfWordsSentences[0], numberOfWordsSentences[1]);
		SuffixArray suffixArray = SuffixArrayFactory.createSuffixArray(corpusArray);

		// Save them
		SuffixArrayFactory.saveVocabulary(vocab, lang, corpusName, outputDirectory);
		SuffixArrayFactory.saveCorpusArray(corpusArray, lang, corpusName, outputDirectory);
		SuffixArrayFactory.saveSuffixArray(suffixArray, lang, corpusName, outputDirectory);
	}
	*/
}

