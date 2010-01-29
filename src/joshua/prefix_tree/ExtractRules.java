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
package joshua.prefix_tree;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.AlignmentGrids;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.alignment.mm.MemoryMappedAlignmentGrids;
import joshua.corpus.mm.MemoryMappedCorpusArray;
import joshua.corpus.suffix_array.FrequentPhrases;
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.suffix_array.mm.MemoryMappedSuffixArray;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.util.Cache;
import joshua.util.io.BinaryIn;


/**
 * Main program to extract hierarchical phrase-based statistical
 * translation rules from an aligned parallel corpus using the
 * suffix array techniques of Lopez (2008).
 *
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
 * @see "Lopez (2008)"
 */
public class ExtractRules {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(ExtractRules.class.getName());

	private String encoding = "UTF-8";

	private String outputFile = "";
	
	private String sourceFileName = "";
	private String sourceSuffixesFileName = "";
	
	private String targetFileName = "";
	private String targetSuffixesFileName = "";
	
	private String alignmentsFileName = "";
	private String commonVocabFileName = "";
	
	private String lexCountsFileName = "";
	
	private String testFileName = "";
	private String frequentPhrasesFileName = "";
	
	private int cacheSize = Cache.DEFAULT_CAPACITY;
	
	private int maxPhraseSpan = 10;
	private int maxPhraseLength = 10;
	private int maxNonterminals = 2;
	private int minNonterminalSpan = 2;
	
	private boolean sentenceInitialX = true;
	private boolean sentenceFinalX = true;
	private boolean edgeXViolates = true;
	
	private boolean requireTightSpans = true;
	
	private boolean binaryCorpus = false;
	
	private String alignmentsType = "AlignmentGrids";
	
	private boolean keepTree = true;
	private int ruleSampleSize = 300;
	private boolean printPrefixTree = false;
	
	private int maxTestSentences = Integer.MAX_VALUE;
	private int startingSentence = 1;
	
	private boolean usePrecomputedFrequentPhrases = true;
	
	public ExtractRules() {
	}
	
	public void setUsePrecomputedFrequentPhrases(boolean usePrecomputedFrequentPhrases) {
		this.usePrecomputedFrequentPhrases = usePrecomputedFrequentPhrases;
	}
	
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	
	public void setAlignmentsFileName(String alignmentsFileName) {
		this.alignmentsFileName = alignmentsFileName;
	}
	
	public void setLexCountsFileName(String lexCountsFileName) {
		this.lexCountsFileName = lexCountsFileName;
	}
	
	public void setStartingSentence(int startingSentence) {
		this.startingSentence = startingSentence;
	}
	
	public void setMaxPhraseSpan(int maxPhraseSpan) {
		this.maxPhraseSpan = maxPhraseSpan;
	}
	
	public void setMaxPhraseLength(int maxPhraseLength) {
		this.maxPhraseLength = maxPhraseLength;
	}
	
	public void setMaxNonterminals(int maxNonterminals) {
		this.maxNonterminals = maxNonterminals;
	}
	
	public void setMinNonterminalSpan(int minNonterminalSpan) {
		this.minNonterminalSpan = minNonterminalSpan;
	}
	
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	public void setMaxTestSentences(int maxTestSentences) {
		this.maxTestSentences = maxTestSentences;
	}
	
	public void setJoshDir(String joshDir) {

		this.sourceFileName = joshDir + File.separator + "source.corpus";
		this.targetFileName = joshDir + File.separator + "target.corpus";
		
		this.commonVocabFileName = joshDir + File.separator + "common.vocab";

		this.lexCountsFileName = joshDir + File.separator + "lexicon.counts";

		this.sourceSuffixesFileName = joshDir + File.separator + "source.suffixes";
		this.targetSuffixesFileName = joshDir + File.separator + "target.suffixes";
		
		this.alignmentsFileName = joshDir + File.separator + "alignment.grids";
		this.alignmentsType = "MemoryMappedAlignmentGrids";
		
		this.frequentPhrasesFileName = joshDir + File.separator + "frequentPhrases";
		
		this.binaryCorpus = true;
	}
	
	public void setTestFile(String testFileName) {
		this.testFileName = testFileName;
	}
	
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setSentenceInitialX(boolean sentenceInitialX) {
		this.sentenceInitialX = sentenceInitialX;
	}
	
	public void setSentenceFinalX(boolean sentenceFinalX) {
		this.sentenceFinalX = sentenceFinalX;
	}
	
	public void setEdgeXViolates(boolean edgeXViolates) {
		this.edgeXViolates = edgeXViolates;
	}
	
	public void setRequireTightSpans(boolean requireTightSpans) {
		this.requireTightSpans = requireTightSpans;
	}
		
	public void setKeepTree(boolean keepTree) {
		this.keepTree = keepTree;
	}
	
	public void setRuleSampleSize(int ruleSampleSize) {
		this.ruleSampleSize = ruleSampleSize;
	}
	
	public void setPrintPrefixTree(boolean printPrefixTree) {
		this.printPrefixTree = printPrefixTree;
	}
	
	
	
	public ParallelCorpusGrammarFactory getGrammarFactory() throws IOException, ClassNotFoundException {
		
		////////////////////////////////
		// Common vocabulary          //
		////////////////////////////////
		if (logger.isLoggable(Level.INFO)) logger.info("Constructing empty common vocabulary");
		Vocabulary commonVocab = new Vocabulary();
		int numSourceWords, numSourceSentences;
		int numTargetWords, numTargetSentences;
		String binaryCommonVocabFileName = this.commonVocabFileName;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Initializing common vocabulary from binary file " + binaryCommonVocabFileName);
			ObjectInput in = BinaryIn.vocabulary(binaryCommonVocabFileName);
			commonVocab.readExternal(in);
			
			numSourceWords = Integer.MIN_VALUE;
			numSourceSentences = Integer.MIN_VALUE;
			
			numTargetWords = Integer.MIN_VALUE;
			numTargetSentences = Integer.MIN_VALUE;
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Initializing common vocabulary with source corpus " + sourceFileName);
			int[] sourceWordsSentences = Vocabulary.initializeVocabulary(sourceFileName, commonVocab, true);
			numSourceWords = sourceWordsSentences[0];
			numSourceSentences = sourceWordsSentences[1];
			
			if (logger.isLoggable(Level.INFO)) logger.info("Initializing common vocabulary with target corpus " + sourceFileName);			
			int[] targetWordsSentences = Vocabulary.initializeVocabulary(targetFileName, commonVocab, true);
			numTargetWords = targetWordsSentences[0];
			numTargetSentences = targetWordsSentences[1];
		}
	
		
		
		//////////////////////////////////
		// Source language corpus array //
		//////////////////////////////////
		final Corpus sourceCorpusArray;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing memory mapped source language corpus array.");
			sourceCorpusArray = new MemoryMappedCorpusArray(commonVocab, sourceFileName);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language corpus array.");
			sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, commonVocab, numSourceWords, numSourceSentences);
		}

		//////////////////////////////////
		// Source language suffix array //
		//////////////////////////////////
		Suffixes sourceSuffixArray;
		String binarySourceSuffixArrayFileName = sourceSuffixesFileName;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from binary file " + binarySourceSuffixArrayFileName);
			sourceSuffixArray = new MemoryMappedSuffixArray(binarySourceSuffixArrayFileName, sourceCorpusArray, cacheSize);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from source corpus.");
			sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, cacheSize);
		}
		
		

				
		//////////////////////////////////
		// Target language corpus array //
		//////////////////////////////////
		final Corpus targetCorpusArray;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing memory mapped target language corpus array.");
			targetCorpusArray = new MemoryMappedCorpusArray(commonVocab, targetFileName);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language corpus array.");
			targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, commonVocab, numTargetWords, numTargetSentences);
		}
		

		//////////////////////////////////
		// Target language suffix array //
		//////////////////////////////////
		Suffixes targetSuffixArray;
		String binaryTargetSuffixArrayFileName = targetSuffixesFileName;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from binary file " + binaryTargetSuffixArrayFileName);
			targetSuffixArray = new MemoryMappedSuffixArray(binaryTargetSuffixArrayFileName, targetCorpusArray, cacheSize);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from target corpus.");
			targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, cacheSize);
		}

		int trainingSize = sourceCorpusArray.getNumSentences();
		if (trainingSize != targetCorpusArray.getNumSentences()) {
			throw new RuntimeException("Source and target corpora have different number of sentences. This is bad.");
		}
		
		
		/////////////////////
		// Alignment data  //
		/////////////////////
		if (logger.isLoggable(Level.INFO)) logger.info("Reading alignment data.");
		final Alignments alignments;
		if ("AlignmentArray".equals(alignmentsType)) {
			if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentArray");
			alignments = SuffixArrayFactory.createAlignments(alignmentsFileName, sourceSuffixArray, targetSuffixArray);
		} else if ("AlignmentGrids".equals(alignmentsType) || "AlignmentsGrid".equals(alignmentsType)) {
			if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentGrids");
			alignments = new AlignmentGrids(new Scanner(new File(alignmentsFileName)), sourceCorpusArray, targetCorpusArray, trainingSize, requireTightSpans);
		} else if ("MemoryMappedAlignmentGrids".equals(alignmentsType)) {
			if (logger.isLoggable(Level.INFO)) logger.info("Using MemoryMappedAlignmentGrids");
			alignments = new MemoryMappedAlignmentGrids(alignmentsFileName, sourceCorpusArray, targetCorpusArray);
		} else {
			alignments = null;
			logger.severe("Invalid alignment type: " + alignmentsType);
			System.exit(-1);
		}
		
		Map<Integer,String> ntVocab = new HashMap<Integer,String>();
		ntVocab.put(SymbolTable.X, SymbolTable.X_STRING);
		
		//////////////////////
		// Lexical Probs    //
		//////////////////////		

//		final LexProbs lexProbs;
		String binaryLexCountsFilename = this.lexCountsFileName;
		
		//////////////////////
		// Frequent Phrases //
		//////////////////////
		if (usePrecomputedFrequentPhrases) {
			logger.info("Reading precomputed frequent phrases from disk");
			FrequentPhrases frequentPhrases = new FrequentPhrases(sourceSuffixArray, frequentPhrasesFileName);
			frequentPhrases.cacheInvertedIndices();
		}


		logger.info("Constructing grammar factory from parallel corpus");
		ParallelCorpusGrammarFactory parallelCorpus;
		if (binaryCorpus) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing lexical translation probabilities from binary file " + binaryLexCountsFilename);
			parallelCorpus = new ParallelCorpusGrammarFactory(sourceSuffixArray, targetSuffixArray, alignments, null, binaryLexCountsFilename, ruleSampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.default_non_terminal, JoshuaConfiguration.oovFeatureCost);
		} else { 
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing lexical translation probabilities from parallel corpus"); 
			parallelCorpus = new ParallelCorpusGrammarFactory(sourceSuffixArray, targetSuffixArray, alignments, null, ruleSampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan, Float.MIN_VALUE, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.default_non_terminal, JoshuaConfiguration.oovFeatureCost);
		}
		return parallelCorpus;
	}


	public void execute() throws IOException, ClassNotFoundException  {

		// Set System.out and System.err to use the provided character encoding
		try {
			System.setOut(new PrintStream(System.out, true, "UTF-8"));
			System.setErr(new PrintStream(System.err, true, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.err.println("UTF-8 is not a valid encoding; using system default encoding for System.out and System.err.");
		} catch (SecurityException e2) {
			System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
		}
		
		PrintStream out;
		if ("-".equals(this.outputFile)) {
			out = System.out;
			logger.info("Rules will be written to standard out");
		} else {
			out = new PrintStream(outputFile,"UTF-8");
			logger.info("Rules will be written to " + outputFile);
		}
		
		ParallelCorpusGrammarFactory parallelCorpus = this.getGrammarFactory();
		
		logger.info("Getting symbol table");
		SymbolTable sourceVocab = parallelCorpus.getSourceCorpus().getVocabulary();
		
		int lineNumber = 0;
		boolean oneTreePerSentence = ! this.keepTree;
		
		logger.info("Will read test sentences from " + testFileName);
		Scanner testFileScanner = new Scanner(new File(testFileName), encoding);
		
		logger.info("Read test sentences from " + testFileName);
		PrefixTree prefixTree = null;
		while (testFileScanner.hasNextLine() && (lineNumber-startingSentence+1)<maxTestSentences) {

			String line = testFileScanner.nextLine();
			lineNumber++;
			if (lineNumber < startingSentence) continue;
			
			int[] words = sourceVocab.getIDs(line);
			
			if (oneTreePerSentence || null==prefixTree) 
			{
//				prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing new prefix tree");
				Node.resetNodeCounter();
				prefixTree = new PrefixTree(parallelCorpus);
				prefixTree.setPrintStream(out);
				prefixTree.sentenceInitialX = this.sentenceInitialX;
				prefixTree.sentenceFinalX   = this.sentenceFinalX;
				prefixTree.edgeXMayViolatePhraseSpan = this.edgeXViolates;
			}
			try {
				if (logger.isLoggable(Level.INFO)) logger.info("Processing source line " + lineNumber + ": " + line);
				prefixTree.add(words);
			} catch (OutOfMemoryError e) {
				logger.warning("Out of memory - attempting to clear cache to free space");
				parallelCorpus.getSuffixArray().getCachedHierarchicalPhrases().clear();
//				targetSuffixArray.getCachedHierarchicalPhrases().clear();
				prefixTree = null;
				System.gc();
				logger.info("Cleared cache and collected garbage. Now attempting to re-construct prefix tree...");
//				prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
				Node.resetNodeCounter();
				prefixTree = new PrefixTree(parallelCorpus);
				prefixTree.setPrintStream(out);
				prefixTree.sentenceInitialX = this.sentenceInitialX;
				prefixTree.sentenceFinalX   = this.sentenceFinalX;
				prefixTree.edgeXMayViolatePhraseSpan = this.edgeXViolates;
				if (logger.isLoggable(Level.INFO)) logger.info("Re-processing source line " + lineNumber + ": " + line);
				prefixTree.add(words);
			}
			
			if (printPrefixTree) {
				System.out.println(prefixTree.toString());
			}
		
//			if (printRules) {
//				if (logger.isLoggable(Level.FINE)) logger.fine("Outputting rules for source line: " + line);
//
//				for (Rule rule : prefixTree.getAllRules()) {
//					String ruleString = rule.toString(ntVocab, sourceVocab, targetVocab);
//					if (logger.isLoggable(Level.FINEST)) logger.finest("Rule: " + ruleString);
//					out.println(ruleString);
//				}
//			}
			
//			if (logger.isLoggable(Level.FINEST)) logger.finest(lexProbs.toString());
			
		
		}
		
		logger.info("Done extracting rules for file " + testFileName);
		
	}
	

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		if (args.length==3) {
			ExtractRules extractRules = new ExtractRules();
			extractRules.setJoshDir(args[0]);
			extractRules.setOutputFile(args[1]);
			extractRules.setTestFile(args[2]);
			extractRules.execute();
		} else if (args.length==5) {
			ExtractRules extractRules = new ExtractRules();
			extractRules.setSourceFileName(args[0]);
			extractRules.setTargetFileName(args[1]);
			extractRules.setAlignmentsFileName(args[2]);
			extractRules.setOutputFile(args[3]);
			extractRules.setTestFile(args[4]);
			extractRules.execute();
		} else {
			System.err.println("Usage: joshDir outputRules testFile");
			System.err.println("---------------OR------------------");
			System.err.println("Usage: source.txt target.txt alignments.txt outputRules testFile");
		}
		
	}

}

