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
package edu.jhu.util.suffix_array;

// Imports
import edu.jhu.joshua.corpus.*;
import edu.jhu.util.sentence.*;
import edu.jhu.util.FileUtil;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/**
 * SuffixArrayFactory is the class that handles the loading and 
 * saving of SuffixArrays and their associated classes.
 * 
 * @author Chris Callison-Burch
 * @since  8 February 2005
 * @version $LastChangedDate$
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
	 * Gets the standard filename for a SuffixArrayServer RMI service.
	 */			 
	 public static String getSuffixArrayServiceName(String lang, String corpus) {
		return (lang+"_"+corpus+"_suffix_array_server");
	 }
	
		 
	/**
	 * Gets the standard filename for an alignments file.
	 */		
	 public static String getAlignmentsFileName(String sourceLang, String targetLang, String corpus) {
		return (sourceLang+"_"+targetLang+"_"+corpus+"_alignments.txt"); 
	 }
	 
	 
	/**
	 * Gets the standard filename for a probable alignments file.
	 */		
	 public static String getProbableAlignmentsFileName(String sourceLang, String targetLang, String corpus) {
		return (sourceLang+"_"+targetLang+"_"+corpus+"_probable_alignments.txt"); 
	 }
		 
		 
	/**
	 * Gets the standard filename for a GridServer RMI service.
	 */			 
	 public static String getGridServiceName(String sourceLang, String targetLang, String corpus) {
		return (sourceLang+"_"+targetLang+"_"+corpus+"_grid_server");
	 }
	
		
	
	/**
	 * Creates a new Vocabulary from a plain text file.
	 * @param inputFilename the plain text file
	 * @param vocab the Vocabulary to instantiate 
	 * @return a tuple containing the 
	 */
	public static int[] createVocabulary(String inputFilename, Vocabulary vocab) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(inputFilename);
		HashSet words = new HashSet();
		int numSentences = 0;
		int numWords = 0;
		while (reader.ready()) {
			Phrase sentence = new Phrase(reader.readLine(), vocab);
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
	 * Creates a new CorpusArray from an ArrayList of sentences.
	 * @param sentences a list of Phrase objects
	 */
	public static CorpusArray createCorpusArray(ArrayList sentences, Vocabulary vocab) {
		// Fix the vocabulary.
		vocab.fixVocabulary();
		vocab.alphabetize();
	
		int numWords = 0;
		
		for(int i = 0; i < sentences.size(); i++) {
			Phrase sentence = (Phrase) sentences.get(i);
			numWords += sentence.size();
		}
		
		int[] corpus =  new int[numWords];
		int[] sentenceIndexes = new int[sentences.size()];
		
		int wordCounter = 0;
		for(int i = 0; i < sentences.size(); i++) {
			Phrase sentence = (Phrase) sentences.get(i);
			sentenceIndexes[i] = wordCounter;
			for(int j = 0; j < sentence.size(); j++) {
				Word word = sentence.getWord(j);
				corpus[wordCounter] = vocab.getID(word);
				wordCounter++;
			}
		}

		return new CorpusArray(corpus, sentenceIndexes, vocab);
	}

	
	/**
	 * Creates a new CorpusArray from a plain text file, given a Vocabulary
	 * created from the same file.
	 * @param numWords the number of words in the file (returned by createVocabulary)
	 * @param numSentences the number of lines in the file (returned by createVocabulary)
	 */
	public static CorpusArray createCorpusArray(String inputFilename, Vocabulary vocab, int numWords, int numSentences) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(inputFilename);
		// initalize the arrays.
		int[] corpus = new int[numWords];
		int[] sentenceIndexes = new int[numSentences];
		
		// instantiate them.
		int wordCounter = 0;
		int sentenceCounter = 0;
		while (reader.ready()) {
			Phrase sentence = new Phrase(reader.readLine(), vocab);
			sentenceIndexes[sentenceCounter] = wordCounter;
			sentenceCounter++;
			
			Iterator jt = sentence.iterator();
			while(jt.hasNext()) {
				Word word = (Word) jt.next();
				corpus[wordCounter] = vocab.getID(word);
				wordCounter++;
			}
			if(SHOW_PROGRESS && sentenceCounter % 10000==0) System.out.println(numWords);
		}
		reader.close();
		return new CorpusArray(corpus, sentenceIndexes, vocab);
	}
	

	
	/**
	 * Creates a new SuffixArray from a CorpusArray created from the same file.
	 */
	public static SuffixArray createSuffixArray(CorpusArray corpusArray) throws IOException {
		return new SuffixArray(corpusArray);
	}


	
	
	
	/**
	 * Writes the Vocabulary to a file.  The file starts with a line which indicates the number of words,
	 * and then contains an alphabetically sorted list of words, one per line.
	 */
	public static void saveVocabulary(Vocabulary vocab, String lang, String corpus, String directory) throws IOException {
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getVocabularyFileName(lang, corpus));

		int vocabSize = vocab.size();
		writer.write(Integer.toString(vocabSize));
		writer.newLine();
		for(int i = 0; i < vocabSize; i++) {
			writer.write(vocab.getWord(i).getString());
			writer.newLine();
		}
		writer.close();
		if(SHOW_PROGRESS) System.out.println("Vocabulary saved!");
	}

	
	 
	 /**
	  * Writes out the corpus in integer representation.  The first line of the
	  * file indicates the number of words and the number of sentences.
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
	  * Writes the suffix array to file.  The first line of the
	  * file indicates the number of words and the number of sentences.
	  */
	public static void saveSuffixArray(SuffixArray suffixArray, String lang, String corpus, String directory) throws IOException {
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, getSuffixArrayFileName(lang, corpus));

		//write out size data on first line
		int numSuffixes = suffixArray.size();
		writer.write(Integer.toString(numSuffixes));
		writer.newLine();
		int suffixNum = 0;
		for(int i = 0; i < numSuffixes; i++) {
			writer.write(Integer.toString(suffixArray.suffixes[i]));
			writer.newLine();
		}
		writer.newLine();
		writer.close();
	}

	/**
	 * Writes a collection of alignment points to a file, with the alignment for each sentence
	 * pair on one line.
	 */
	public static void saveAlignments(Grids grids, String sourceLang, String targetLang, String corpusName, String directory) throws IOException {
		String alignmentsFilename = getAlignmentsFileName(sourceLang, targetLang, corpusName);
		
		BufferedWriter writer = FileUtil.getBufferedWriter(directory, alignmentsFilename);
		for(int i = 0; i < grids.size(); i++) {
			Grid grid = grids.getGrid(i);
			if(grid != null) {
				FileUtil.writeLine(grid.toString(), writer);
			} else {
				FileUtil.writeLine("", writer);
			}
		}
		writer.flush();
		writer.close();
	}


	/**
	 * Loads a Vocabulary from a file containing an alphabetized list of words, one on each line. 
	 * The first of the file line contains the number of words.
	 */
	public static Vocabulary loadVocabulary(String lang, String corpus, String directory) throws IOException {
		BufferedReader reader = FileUtil.getBufferedReader(directory, getVocabularyFileName(lang, corpus));
		int vocabSize = Integer.parseInt(reader.readLine());
		Vocabulary vocab = new Vocabulary();
		
		int i = 0;
		while(reader.ready()) {
			Word word = new Word(reader.readLine(), vocab);
			if(SHOW_PROGRESS && i % 10000==0) System.out.println(i);
			i++;
		}
		reader.close();
		vocab.fixVocabulary();
		return vocab;
	}
	
	
	 /**
	  * Reads in a corpus that is already in integer representation.  Assumes that the
	  * first line indicates the number of words and the number of sentences, as ouput
	  * by saveCorpusArray.
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
			StringTokenizer line = new StringTokenizer(reader.readLine(), Phrase.SPACE_CHARACTERS);
			sentences[sentenceCounter++] = wordCounter;
			while (line.hasMoreTokens()) {
				corpus[wordCounter++] = Integer.parseInt(line.nextToken());
			}
		}
		reader.close();
		return new CorpusArray(corpus, sentences, vocab);
	}




	 /**
	  * Reads a sorted suffix array from a file.  The first line of the
	  * file indicates the number of suffixes.
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
	  * Reads in a file containing alignment grids.
	  * @param sourceCorpus source sentence lengths are used to ensure the width of each Grid is long enough
	  * @param targetCorpus target sentence lengths are used to ensure the height of each Grid is long enough
	  * @returns an ArrayList of Grid objects
	  */
	public static Grids loadAlignments(String sourceLang, String targetLang, String corpusName, String directory, 
										Corpus sourceCorpus, Corpus targetCorpus) throws IOException {
		String alignmentsFilename = getAlignmentsFileName(sourceLang, targetLang, corpusName);
		String reverseAlignmentsFilename = getAlignmentsFileName(targetLang, sourceLang, corpusName);
		
		boolean shouldTranspose = false;
		if(!(FileUtil.exists(directory, alignmentsFilename) || FileUtil.exists(directory, alignmentsFilename + ".gz")) && 
			(FileUtil.exists(directory, reverseAlignmentsFilename) || FileUtil.exists(directory, reverseAlignmentsFilename + ".gz"))) {
			alignmentsFilename = reverseAlignmentsFilename;
			shouldTranspose = true;
			Corpus tmpCorpus = sourceCorpus;
			sourceCorpus = targetCorpus;
			targetCorpus = tmpCorpus;
		}
		
		ArrayList alignments = new ArrayList();
		BufferedReader reader = FileUtil.getBufferedReader(directory, alignmentsFilename);
		int counter = 0;
		while(reader.ready() && counter < sourceCorpus.getNumSentences()) {
			int sourceLength = sourceCorpus.getSentence(counter).size();
			int targetLength = targetCorpus.getSentence(counter).size();
			String alignmentsStr = reader.readLine();
			Grid grid = new Grid(sourceLength, targetLength, alignmentsStr);
			alignments.add(grid);
			counter++;
		}
		reader.close();
		
		// if we don't have alignments for all of the sentences in the corpora,
		// then create empty alignments for the remainder
		for(int i = counter; i < sourceCorpus.getNumSentences(); i++) {
			int sourceLength = sourceCorpus.getSentence(counter).size();
			int targetLength = targetCorpus.getSentence(counter).size();
			Grid grid = new Grid(sourceLength, targetLength);
			alignments.add(grid);
		}
		
		return new Grids(alignments, shouldTranspose);
	}

	 	

		
//===============================================================
// Main 
//===============================================================
	
	public static void main(String[] args) throws java.io.IOException {
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
}

