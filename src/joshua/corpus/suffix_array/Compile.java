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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.AlignedParallelCorpus;
import joshua.corpus.CorpusArray;
import joshua.corpus.ParallelCorpus;
import joshua.corpus.alignment.AlignmentGrids;
import joshua.corpus.lexprob.LexProbs;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.util.Cache;
import joshua.util.io.BinaryOut;

/**
 * Compiles a parallel corpus into binary data files.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class Compile {

	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(Compile.class.getName());
	
	
	private String sourceCorpusFileName;
	private String targetCorpusFileName;
	private String alignmentsFileName;
	private String outputDirName;
	private String charset = "UTF-8";
	
	private int minFrequency = 0;
	private short maxPhrases = 100;

	private int maxPhraseLength = JoshuaConfiguration.sa_max_phrase_length;
	
	private int maxPhraseSpan = JoshuaConfiguration.sa_max_phrase_span;
	
	private int minNonterminalSpan = JoshuaConfiguration.sa_min_nonterminal_span;
	
	public void setMinNonterminalSpan(int minNonterminalSpan) {
		this.minNonterminalSpan = minNonterminalSpan;
	}
	
	public void setMaxPhraseSpan(int maxPhraseSpan) {
		this.maxPhraseSpan = maxPhraseSpan;
	}
	
	public void setMinFrequency(int minFrequency) {
		this.minFrequency = minFrequency;
	}
	
	public void setMaxPhrases(short maxPhrases) {
		this.maxPhrases = maxPhrases;
	}
	
	public void setMaxPhraseLength(int maxPhraseLength) {
		this.maxPhraseLength = maxPhraseLength;
	}
	
	public void setSourceCorpus(String sourceCorpusFileName) {
		this.sourceCorpusFileName = sourceCorpusFileName;
	}
	
	public void setTargetCorpus(String targetCorpusFileName) {
		this.targetCorpusFileName = targetCorpusFileName;
	}
	
	public void setAlignments(String alignmentsFileName) {
		this.alignmentsFileName = alignmentsFileName;
	}
	
	
	public void setOutputDir(String outputDirName) {
		this.outputDirName = outputDirName;
	}
	
	public void setEncoding(String charset) {
		this.charset = charset;
	}
	
	public void execute() throws IOException {
		// Verify that output directory exists or can be created
		File outputDir = new File(outputDirName);
		if (! outputDir.exists()) {
			boolean success = outputDir.mkdirs();
			if (! success) {
				logger.severe("Output directory does not exist, and could not be successfully created: " + outputDirName);
				System.exit(-1);
			}
		} else if (! outputDir.isDirectory()) {
			logger.severe("Output directory exists, but is not a directory: " + outputDirName);
			System.exit(-2);
		} else if (! outputDirName.endsWith(".josh")) {
			logger.warning("By convention, the output directory should end in .josh");
		}
		
		
		// Construct common vocabulary
		Vocabulary symbolTable = new Vocabulary();
		if (logger.isLoggable(Level.INFO)) logger.info("Adding terminal tokens from file " + sourceCorpusFileName + " to common vocabulary");
		int[] sourceLengths = Vocabulary.initializeVocabulary(sourceCorpusFileName, symbolTable, false);
		if (logger.isLoggable(Level.INFO)) logger.info("Adding terminal tokens from file " + targetCorpusFileName + " to common vocabulary");
		int[] targetLengths = Vocabulary.initializeVocabulary(targetCorpusFileName, symbolTable, true);
		
		if (sourceLengths[1] != targetLengths[1]) {
			logger.severe("Source corpus and target corpus have different number of sentences (" + sourceLengths[1] + " vs " + targetLengths[1] + ")");
			System.exit(-3);
		}
		int numberOfSentences = sourceLengths[1];
		
		// Write README file to disk
		String readmeFilename = outputDirName + File.separator + "README.txt";
		PrintStream out = new PrintStream(readmeFilename);

		out.println("This directory contains the following binary files:");
		out.println();

		
		
		// Write vocabulary to disk
		{
			String binaryVocabFilename = outputDirName + File.separator + "common.vocab";
			if (logger.isLoggable(Level.INFO)) logger.info("Writing binary common vocabulary to disk at " + binaryVocabFilename);
			
			ObjectOutput vocabOut =
	    		new BinaryOut(new FileOutputStream(binaryVocabFilename), true);
			symbolTable.setExternalizableEncoding(charset);
	    	symbolTable.writeExternal(vocabOut);
	    	vocabOut.flush();
	    	
			out.println("Common symbol table for source and target language: " + binaryVocabFilename);
		}
		
		
		
		// Construct source language corpus
		if (logger.isLoggable(Level.INFO)) logger.info("Constructing corpus array from file " + sourceCorpusFileName);
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceCorpusFileName, symbolTable, sourceLengths[0], sourceLengths[1]);
		
		// Write source corpus to disk
		{
			String binarySourceCorpusFilename = outputDirName + File.separator + "source.corpus";
			if (logger.isLoggable(Level.INFO)) logger.info("Writing binary source corpus to disk at " + binarySourceCorpusFilename);
			
	    	BinaryOut corpusOut = new BinaryOut(new FileOutputStream(binarySourceCorpusFilename), false);
	    	sourceCorpusArray.writeExternal(corpusOut);	
	    	corpusOut.flush();
	    	
			out.println("Source language corpus: " + binarySourceCorpusFilename);
		}
		
		// Construct target language corpus
		if (logger.isLoggable(Level.INFO)) logger.info("Constructing corpus array from file " + targetCorpusFileName);
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetCorpusFileName, symbolTable, targetLengths[0], targetLengths[1]);
		
		
		// Write target language corpus to disk
		{
			String binaryTargetCorpusFilename = outputDirName + File.separator + "target.corpus";
			if (logger.isLoggable(Level.INFO)) logger.info("Writing binary target corpus to disk at " + binaryTargetCorpusFilename);
			
	    	BinaryOut corpusOut = new BinaryOut(new FileOutputStream(binaryTargetCorpusFilename), false);
	    	targetCorpusArray.writeExternal(corpusOut);	
	    	corpusOut.flush();
	    	
	    	out.println("Target language corpus: " + binaryTargetCorpusFilename);
		}
		
		{
			// Construct alignments data structure
			AlignmentGrids grids = new AlignmentGrids(
					new Scanner(new File(alignmentsFileName)), 
					sourceCorpusArray, 
					targetCorpusArray,
					numberOfSentences);

			// Write alignments to disk
			{
				String binaryAlignmentsFilename = outputDirName + File.separator + "alignment.grids";
				if (logger.isLoggable(Level.INFO)) logger.info("Writing binary alignment grids to disk at " + binaryAlignmentsFilename);

				BinaryOut alignmentsOut = new BinaryOut(binaryAlignmentsFilename);
				grids.writeExternal(alignmentsOut);
				alignmentsOut.flush();
				alignmentsOut.close();

				out.println("Source-target alignment grids: " + binaryAlignmentsFilename);
			}

			// Write lexprobs to disk
			{
				ParallelCorpus parallelCorpus = new AlignedParallelCorpus(sourceCorpusArray, targetCorpusArray, grids);

				if (logger.isLoggable(Level.INFO)) logger.info("Constructing lexprob table");
				LexicalProbabilities lexProbs = 
					new LexProbs(parallelCorpus, Float.MIN_VALUE);

				String lexprobsFilename = outputDirName + File.separator + "lexprobs.txt";
				FileOutputStream stream = new FileOutputStream(lexprobsFilename);
				OutputStreamWriter lexprobsOut = new OutputStreamWriter(stream, charset);

				String binaryLexCountFilename = outputDirName + File.separator + "lexicon.counts";
				if (logger.isLoggable(Level.INFO)) logger.info("Writing binary lexicon counts to disk at " + binaryLexCountFilename);

				//			BinaryOut lexCountOut = new BinaryOut(binaryLexCountFilename);
				ObjectOutput lexCountOut = new ObjectOutputStream(new FileOutputStream(binaryLexCountFilename));
				lexProbs.writeExternal(lexCountOut);
				lexCountOut.close();

				String s = lexProbs.toString();

				if (logger.isLoggable(Level.INFO)) logger.info("Writing lexprobs at " + lexprobsFilename);
				lexprobsOut.write(s);  
				lexprobsOut.flush();
				lexprobsOut.close();
				out.println("Lexprobs at " + lexprobsFilename);

			}
		}
		
		// Write target language suffix array to disk
		{
			// Construct target language suffix array
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing suffix array from file " + targetCorpusFileName);
			SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, Cache.DEFAULT_CAPACITY);
			
			String binaryTargetSuffixesFilename = outputDirName + File.separator + "target.suffixes";
			if (logger.isLoggable(Level.INFO)) logger.info("Writing binary target corpus to disk at " + binaryTargetSuffixesFilename);
			
			BinaryOut suffixesOut = new BinaryOut(new FileOutputStream(binaryTargetSuffixesFilename), false);
			targetSuffixArray.writeExternal(suffixesOut);	
	    	suffixesOut.flush();
	    	
			out.println("Target language suffix array: " + binaryTargetSuffixesFilename);
		}
		
		
		{
			// Construct source language suffix array
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing suffix array from file " + sourceCorpusFileName);
			SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, Cache.DEFAULT_CAPACITY);

			// Write source language suffix array to disk
			{
				String binarySourceSuffixesFilename = outputDirName + File.separator + "source.suffixes";
				if (logger.isLoggable(Level.INFO)) logger.info("Writing binary source corpus to disk at " + binarySourceSuffixesFilename);

				BinaryOut suffixesOut = new BinaryOut(new FileOutputStream(binarySourceSuffixesFilename), false);
				sourceSuffixArray.writeExternal(suffixesOut);	
				suffixesOut.flush();

				out.println("Source language suffix array: " + binarySourceSuffixesFilename);
			}

			// Precompute and write frequent phrase locations to disk
			{
				if (logger.isLoggable(Level.INFO)) logger.info("Precomputing indices for most frequent phrases");
				FrequentPhrases frequentPhrases = 
					new FrequentPhrases(sourceSuffixArray, minFrequency, maxPhrases, maxPhraseLength, maxPhraseLength, maxPhraseSpan, minNonterminalSpan);

				String frequentPhrasesFilename = outputDirName + File.separator + "frequentPhrases";
				if (logger.isLoggable(Level.INFO)) logger.info("Writing precomputing indices for most frequent phrases at " + frequentPhrasesFilename);
				BinaryOut frequentPhrasesOut = new BinaryOut(frequentPhrasesFilename);
				frequentPhrases.writeExternal(frequentPhrasesOut);
				frequentPhrasesOut.close();
			}
		}
		
		out.flush();
		out.close();
		
		if (logger.isLoggable(Level.INFO)) logger.info("Completed writing binary files to disk");
		
	}
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 4) {
			System.err.println("Usage: java " + Compile.class.getName() + " sourceCorpus targetCorpus alignmentsFile outputDir.josh");
			System.exit(0);
		}
		
		Compile compiler = new Compile();
		compiler.setSourceCorpus(args[0]);
		compiler.setTargetCorpus(args[1]);
		compiler.setAlignments(args[2]);
		compiler.setOutputDir(args[3]);
		if (args.length > 4) compiler.setEncoding(args[4]);
		
		compiler.execute();
	}
	
}
