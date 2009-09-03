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
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.suffix_array.mm.MemoryMappedSuffixArray;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
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
	private String sourceVocabFileName = "";
	private String targetVocabFileName = "";
	
	private String testFileName = "";
	
	
	private int cacheSize = Cache.DEFAULT_CAPACITY;
	
	private int maxPhraseSpan = 10;
	private int maxPhraseLength = 10;
	private int maxNonterminals = 2;
	private int minNonterminalSpan = 2;
	
	private boolean sentenceInitialX = true;
	private boolean sentenceFinalX = true;
	private boolean edgeXViolates = true;
	
	private boolean requireTightSpans = true;
	
	private boolean binarySource = true;
	private boolean binaryTarget = true;
	
	private String alignmentsType = "MemoryMappedAlignmentGrids";
	
	private boolean keepTree = true;
	private int ruleSampleSize = 300;
	private boolean printPrefixTree = false;
	
	private int maxTestSentences = Integer.MAX_VALUE;
	private int startingSentence = 1;
	
	public ExtractRules() {
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
		
		this.sourceVocabFileName = joshDir + File.separator + "common.vocab";
		this.targetVocabFileName = joshDir + File.separator + "common.vocab";
		
		this.sourceSuffixesFileName = joshDir + File.separator + "source.suffixes";
		this.targetSuffixesFileName = joshDir + File.separator + "target.suffixes";
		
		this.alignmentsFileName = joshDir + File.separator + "alignment.grids";
		this.alignmentsType = "MemoryMappedAlignmentGrids";
		
		this.binarySource = true;
		this.binaryTarget = true;
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
	
//	private void setBinarySource(boolean binarySource) {
//		this.binarySource = binarySource;
//	}
//	
//	private void setSourceFile(String sourceFileName) {
//		this.sourceFileName = sourceFileName;
//	}
//	
//	private void setBinaryTarget(boolean binaryTarget) {
//		this.binaryTarget = binaryTarget;
//	}
//	
//	private void setTargetFile(String targetFileName) {
//		this.targetFileName = targetFileName;
//	}
//	
//	private void setAlignmentsFile(String alignmentsFileName) {
//		this.alignmentsFileName = alignmentsFileName;
//	}
//	
//	private void setSourceVocab(String vocabFileName) {
//		this.sourceVocabFileName = vocabFileName;
//	}
//	
//	private void setTargetVocab(String vocabFileName) {
//		this.targetVocabFileName = vocabFileName;
//	}
//	
//	private void setAlignmentsType(String alignmentsType) {
//		this.alignmentsType = alignmentsType;
//	}
	
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
		// Source language vocabulary //
		////////////////////////////////
		int numSourceWords, numSourceSentences;
		Vocabulary sourceVocab = new Vocabulary();
		String binarySourceVocabFileName = this.sourceVocabFileName;
		if ( binarySourceVocabFileName.equals("")) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language vocabulary from source corpus " + sourceFileName);
			int[] sourceWordsSentences = Vocabulary.initializeVocabulary(sourceFileName, sourceVocab, true);
			numSourceWords = sourceWordsSentences[0];
			numSourceSentences = sourceWordsSentences[1];
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language vocabulary from binary file " + binarySourceVocabFileName);
			ObjectInput in = BinaryIn.vocabulary(binarySourceVocabFileName);
			sourceVocab.readExternal(in);
			numSourceWords = Integer.MIN_VALUE;
			numSourceSentences = Integer.MIN_VALUE;
		}
		
		//////////////////////////////////
		// Source language corpus array //
		//////////////////////////////////
		final Corpus sourceCorpusArray;
		if (binarySource) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing memory mapped source language corpus array.");
			sourceCorpusArray = new MemoryMappedCorpusArray(sourceVocab, sourceFileName);
		} else if (numSourceSentences==Integer.MIN_VALUE || numSourceWords==Integer.MIN_VALUE) {
			sourceCorpusArray = null;
			logger.severe("If a binary source vocab file is specified, the corresponding source corpus must also be a binary file.");
			System.exit(-1);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language corpus array.");
			sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, numSourceWords, numSourceSentences);
		}

		//////////////////////////////////
		// Source language suffix array //
		//////////////////////////////////
		Suffixes sourceSuffixArray;
		String binarySourceSuffixArrayFileName = sourceSuffixesFileName;
		if (binarySourceSuffixArrayFileName.equals("")) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from source corpus.");
			sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, cacheSize);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from binary file " + binarySourceSuffixArrayFileName);
			sourceSuffixArray = new MemoryMappedSuffixArray(binarySourceSuffixArrayFileName, sourceCorpusArray, cacheSize);
		}
		
		
		////////////////////////////////
		// Target language vocabulary //
		////////////////////////////////
		int numTargetWords, numTargetSentences;
		Vocabulary targetVocab = new Vocabulary();

		String binaryTargetVocabFileName = this.targetVocabFileName;
		if ( binaryTargetVocabFileName.equals("")) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language vocabulary from target corpus " + targetFileName);		
//			targetFileName = commandLine.getValue(target);
			int[] targetWordsSentences = Vocabulary.initializeVocabulary(targetFileName, targetVocab, true);
			numTargetWords = targetWordsSentences[0];
			numTargetSentences = targetWordsSentences[1];
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language vocabulary from binary file " + binaryTargetVocabFileName);
			ObjectInput in = BinaryIn.vocabulary(binaryTargetVocabFileName);
			targetVocab.readExternal(in);
			numTargetWords = Integer.MIN_VALUE;
			numTargetSentences = Integer.MIN_VALUE;
		}
				
		//////////////////////////////////
		// Target language corpus array //
		//////////////////////////////////
		final Corpus targetCorpusArray;
		if (binaryTarget) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing memory mapped target language corpus array.");
			targetCorpusArray = new MemoryMappedCorpusArray(targetVocab, targetFileName);
		} else if (numTargetSentences==Integer.MIN_VALUE || numTargetWords==Integer.MIN_VALUE) {
			targetCorpusArray = null;
			logger.severe("If a binary target vocab file is specified, the corresponding target corpus must also be a binary file.");
			System.exit(-1);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language corpus array.");
			targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, numTargetWords, numTargetSentences);
		}
		

		//////////////////////////////////
		// Target language suffix array //
		//////////////////////////////////
		Suffixes targetSuffixArray;
		String binaryTargetSuffixArrayFileName = targetSuffixesFileName;
		if (binaryTargetSuffixArrayFileName.equals("")) {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from target corpus.");
			targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, cacheSize);
		} else {
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from binary file " + binaryTargetSuffixArrayFileName);
			targetSuffixArray = new MemoryMappedSuffixArray(binaryTargetSuffixArrayFileName, targetCorpusArray, cacheSize);
		}

		int trainingSize = sourceCorpusArray.getNumSentences();
		if (trainingSize != targetCorpusArray.getNumSentences()) {
			throw new RuntimeException("Source and target corpora have different number of sentences. This is bad.");
		}
		
		
		/////////////////////
		// Alignment data  //
		/////////////////////
		if (logger.isLoggable(Level.INFO)) logger.info("Reading alignment data.");
//		String alignmentFileName = commandLine.getValue(alignment);
		final Alignments alignments;
//		String alignmentsType = commandLine.getValue(alignmentType);
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
		
//		ParallelCorpus parallelCorpus = 
//			new AlignedParallelCorpus(sourceCorpusArray, targetCorpusArray, alignments);
//		
//		LexicalProbabilities lexProbs = 
//			new LexProbs(parallelCorpus, Float.MIN_VALUE);

		Map<Integer,String> ntVocab = new HashMap<Integer,String>();
		ntVocab.put(SymbolTable.X, SymbolTable.X_STRING);

	
		
//		logger.info("Scanner has line == " +testFileScanner.hasNextLine());
//		PrefixTree.SENTENCE_INITIAL_X = this.sentenceInitialX;//commandLine.getValue(sentence_initial_X);
//		PrefixTree.SENTENCE_FINAL_X   = this.sentenceFinalX; // commandLine.getValue(sentence_final_X);
//		
//		PrefixTree.EDGE_X_MAY_VIOLATE_PHRASE_SPAN = this.edgeXViolates; //commandLine.getValue(edge_X_violates);
		

//		RuleExtractor ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, ruleSampleSize, maxPhraseSpan, maxPhraseLength, minNonterminalSpan, maxPhraseSpan);
		
		//commandLine.getValue(keepTree);
		
		logger.info("Constructing grammar factory from parallel corpus");
		ParallelCorpusGrammarFactory parallelCorpus = new ParallelCorpusGrammarFactory(sourceSuffixArray, targetSuffixArray, alignments, null, ruleSampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan, Float.MIN_VALUE);
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
		
		if (args.length != 3) {
			System.err.println("Usage: joshDir outputRules testFile");
		} else {
					
			ExtractRules extractRules = new ExtractRules();
			extractRules.setJoshDir(args[0]);
			extractRules.setOutputFile(args[1]);
			extractRules.setTestFile(args[2]);
			extractRules.execute();
			
		}
		
	}

}

