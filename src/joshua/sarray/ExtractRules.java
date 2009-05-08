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
package joshua.sarray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Vocabulary;
import joshua.corpus.alignment.AlignmentGrids;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.alignment.MemoryMappedAlignmentGrids;
import joshua.corpus.lexprob.SampledLexProbs;
import joshua.decoder.ff.tm.Rule;
import joshua.sarray.mm.MemoryMappedCorpusArray;
import joshua.sarray.mm.MemoryMappedSuffixArray;
import joshua.util.Cache;
import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;
import joshua.util.io.BinaryIn;


/**
 * Main program to extract hierarchical phrase-based statistical translation rules
 * from an aligned parallel corpus using the suffix array techniques of Lopez (2008).
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
 * @see Lopez (2008)
 */
public class ExtractRules {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(ExtractRules.class.getName());


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		boolean finalConfirmation = false;
		
		try {
			CommandLineParser commandLine = new CommandLineParser();

			Option<String> source = commandLine.addStringOption('f',"source","SOURCE_FILE","Source language training file");
			Option<Boolean> binarySource = commandLine.addBooleanOption("binary-source",false,"Is source corpus file in binary memory-mappable format");
			Option<String> sourceSuffixes = commandLine.addStringOption("source-suffixes","SUFFIXES_FILE","","Binary memory-mappable source language training file suffix array");
			Option<String> sourceSymbols = commandLine.addStringOption("source-vocab","VOCAB_FILE","","Binary memory-mappable source language training file vocabulary");
			
			
			Option<String> target = commandLine.addStringOption('e',"target","TARGET_FILE","Target language training file");		
			Option<Boolean> binaryTarget = commandLine.addBooleanOption("binary-target",false,"Is target corpus file in binary memory-mappable format");
			Option<String> targetSuffixes = commandLine.addStringOption("target-suffixes","SUFFIXES_FILE","","Binary memory-mappable target language training file suffix array");
			Option<String> targetSymbols = commandLine.addStringOption("target-vocab","VOCAB_FILE","","Binary memory-mappable target language training file vocabulary");
			
			
			Option<String> alignment = commandLine.addStringOption('a',"alignments","ALIGNMENTS_FILE","Source-target alignments training file");	
			Option<String> alignmentType = commandLine.addStringOption("alignmentsType","ALIGNMENT_TYPE","AlignmentGrids","Type of alignment data structure");
						
			
			Option<String> test = commandLine.addStringOption('t',"test","TEST_FILE","Source language test file");

			Option<String> output = commandLine.addStringOption('o',"output","OUTPUT_FILE","-","Output file");

			Option<String> encoding = commandLine.addStringOption("encoding","ENCODING","UTF-8","File encoding format");

			Option<Integer> lexSampleSize = commandLine.addIntegerOption("lexSampleSize","LEX_SAMPLE_SIZE",1000, "Size to use when sampling for lexical probability calculations");
			Option<Integer> ruleSampleSize = commandLine.addIntegerOption("ruleSampleSize","RULE_SAMPLE_SIZE",300, "Maximum number of rules to store at each node in the prefix tree");

			Option<Integer> maxPhraseSpan = commandLine.addIntegerOption("maxPhraseSpan","MAX_PHRASE_SPAN",10, "Maximum span in the training corpus of any extracted source phrase");
			Option<Integer> maxPhraseLength = commandLine.addIntegerOption("maxPhraseLength","MAX_PHRASE_LENGTH",5, "Maximum number of terminals plus nonterminals allowed in the source side of a phrase");
			Option<Integer> maxNonterminals = commandLine.addIntegerOption("maxNonterminals","MAX_NONTERMINALS",2, "Max nonterminals");
			Option<Integer> minNonterminalSpan = commandLine.addIntegerOption("minNonterminalSpan","MIN_NONTERMINAL_SPAN", 2, "Minimum nonterminal span");
			
			Option<Integer> cacheSize = commandLine.addIntegerOption("cache","CACHE",1000, "Max number of patterns for which to cache hierarchical phrases");

			Option<Boolean> output_gz = commandLine.addBooleanOption("output-gzipped",false,"should the output file be gzipped");

			Option<Boolean> sentence_initial_X = commandLine.addBooleanOption("sentence-initial-X",true,"should rules with initial X be extracted from sentence-initial phrases");
			Option<Boolean> sentence_final_X = commandLine.addBooleanOption("sentence-final-X",true,"should rules with final X be extracted from sentence-final phrases");

			Option<Boolean> edge_X_violates = commandLine.addBooleanOption("violating-X",true,"should rules with initial X or final X be extracted in cases where doing so would violate the maximum phrase span constraint");
			
			Option<Boolean> print_prefixTree = commandLine.addBooleanOption("print-prefix-tree",false,"should prefix tree be printed to standard out (for debugging)");
			Option<Boolean> print_rules = commandLine.addBooleanOption("print-rules",true,"should extracted rules be printed to standard out");
			
			Option<Boolean> confirm = commandLine.addBooleanOption("confirm",false,"should program pause for user input before constructing prefix trees?");
			Option<Boolean> keepTree = commandLine.addBooleanOption("keepTree",false,"should a single prefix tree be used (instead of one per sentence)?");
			Option<Boolean> requireTightSpans = commandLine.addBooleanOption("tightSpans",true,"Require tightly aligned spans");
			
			
			commandLine.parse(args);

			if (commandLine.getValue(confirm)==true) {
				finalConfirmation = true;
			}

			// Set System.out and System.err to use the provided character encoding
			try {
				System.setOut(new PrintStream(System.out, true, commandLine.getValue(encoding)));
				System.setErr(new PrintStream(System.err, true, commandLine.getValue(encoding)));
			} catch (UnsupportedEncodingException e1) {
				System.err.println(commandLine.getValue(encoding) + " is not a valid encoding; using system default encoding for System.out and System.err.");
			} catch (SecurityException e2) {
				System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
			}

			// Lane - TODO -
			//SuffixArray.INVERTED_INDEX_PRECOMPUTATION_MIN_FREQ = commandLine.getValue("CACHE_PRECOMPUTATION_FREQUENCY_THRESHOLD");

			int maxCacheSize = commandLine.getValue(cacheSize);
			if (logger.isLoggable(Level.INFO)) logger.info("Suffix array will cache hierarchical phrases for at most " + maxCacheSize + " patterns.");
			
			
			////////////////////////////////
			// Source language vocabulary //
			////////////////////////////////
			int numSourceWords, numSourceSentences;
			Vocabulary sourceVocab = new Vocabulary();
			String sourceFileName = commandLine.getValue(source);
			String binarySourceVocabFileName = commandLine.getValue(sourceSymbols);
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
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			
			
			//////////////////////////////////
			// Source language corpus array //
			//////////////////////////////////
			Corpus sourceCorpusArray;
			if (commandLine.getValue(binarySource)) {
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
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			//////////////////////////////////
			// Source language suffix array //
			//////////////////////////////////
			Suffixes sourceSuffixArray;
			String binarySourceSuffixArrayFileName = commandLine.getValue(sourceSuffixes);
			if (binarySourceSuffixArrayFileName.equals("")) {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from source corpus.");
				sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, maxCacheSize);
			} else {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language suffix array from binary file " + binarySourceSuffixArrayFileName);
				sourceSuffixArray = new MemoryMappedSuffixArray(binarySourceSuffixArrayFileName, sourceCorpusArray, maxCacheSize);
			}
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			
			
			////////////////////////////////
			// Target language vocabulary //
			////////////////////////////////
			int numTargetWords, numTargetSentences;
			Vocabulary targetVocab = new Vocabulary();
			String targetFileName = commandLine.getValue(target);

			String binaryTargetVocabFileName = commandLine.getValue(targetSymbols);
			if ( binaryTargetVocabFileName.equals("")) {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language vocabulary from target corpus " + targetFileName);		
				targetFileName = commandLine.getValue(target);
				int[] targetWordsSentences = Vocabulary.initializeVocabulary(commandLine.getValue(target), targetVocab, true);
				numTargetWords = targetWordsSentences[0];
				numTargetSentences = targetWordsSentences[1];
			} else {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language vocabulary from binary file " + binaryTargetVocabFileName);
				ObjectInput in = BinaryIn.vocabulary(binaryTargetVocabFileName);
				targetVocab.readExternal(in);
				numTargetWords = Integer.MIN_VALUE;
				numTargetSentences = Integer.MIN_VALUE;
			}
			
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			
			
			//////////////////////////////////
			// Target language corpus array //
			//////////////////////////////////
			Corpus targetCorpusArray;
			if (commandLine.getValue(binaryTarget)) {
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
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}
			

			//////////////////////////////////
			// Target language suffix array //
			//////////////////////////////////
			Suffixes targetSuffixArray;
			String binaryTargetSuffixArrayFileName = commandLine.getValue(targetSuffixes);
			if (binaryTargetSuffixArrayFileName.equals("")) {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from target corpus.");
				targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, maxCacheSize);
			} else {
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array from binary file " + binaryTargetSuffixArrayFileName);
				targetSuffixArray = new MemoryMappedSuffixArray(binaryTargetSuffixArrayFileName, targetCorpusArray, maxCacheSize);
			}
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			int trainingSize = sourceCorpusArray.getNumSentences();
			if (trainingSize != targetCorpusArray.getNumSentences()) {
				throw new RuntimeException("Source and target corpora have different number of sentences. This is bad.");
			}
			
			
			/////////////////////
			// Alignment data  //
			/////////////////////
			if (logger.isLoggable(Level.INFO)) logger.info("Reading alignment data.");
			String alignmentFileName = commandLine.getValue(alignment);
			Alignments alignments;
			String alignmentsType = commandLine.getValue(alignmentType);
			if ("AlignmentArray".equals(alignmentsType)) {
				if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentArray");
				alignments = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);
			} else if ("AlignmentGrids".equals(alignmentsType) || "AlignmentsGrid".equals(alignmentsType)) {
				if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentGrids");
				alignments = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, trainingSize, commandLine.getValue(requireTightSpans));
			} else if ("MemoryMappedAlignmentGrids".equals(alignmentsType)) {
				if (logger.isLoggable(Level.INFO)) logger.info("Using MemoryMappedAlignmentGrids");
				alignments = new MemoryMappedAlignmentGrids(alignmentFileName, sourceCorpusArray, targetCorpusArray);
			} else {
				alignments = null;
				logger.severe("Invalid alignment type: " + commandLine.getValue(alignmentType));
				System.exit(-1);
			}
			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			PrintStream out;
			if ("-".equals(commandLine.getValue(output))) {
				out = System.out;
			} else if (commandLine.getValue(output).endsWith(".gz") || commandLine.getValue(output_gz)) {
				//XXX This currently doesn't work
				out = new PrintStream(new GZIPOutputStream(new FileOutputStream(commandLine.getValue(output))));
				System.err.println("GZIP output not currently working properly");
				System.exit(-1);
			} else {
				out = new PrintStream(commandLine.getValue(output));
			}

			if (logger.isLoggable(Level.INFO)) logger.info("Constructing lexical probabilities table");

			SampledLexProbs lexProbs = 
				new SampledLexProbs(commandLine.getValue(lexSampleSize), sourceSuffixArray, targetSuffixArray, alignments, SuffixArray.DEFAULT_CACHE_CAPACITY, false);
			
			if (logger.isLoggable(Level.INFO)) logger.info("Done constructing lexical probabilities table");

			if (commandLine.getValue(confirm)) {
			    if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
			    System.in.read();
			}

			if (logger.isLoggable(Level.INFO)) logger.info("Should store a max of " + commandLine.getValue(ruleSampleSize) + " rules at each node in a prefix tree.");

			Map<Integer,String> ntVocab = new HashMap<Integer,String>();
			ntVocab.put(PrefixTree.X, "X");

			Scanner testFileScanner = new Scanner(new File(commandLine.getValue(test)), commandLine.getValue(encoding));

			if (commandLine.getValue(confirm)) {
				if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
				System.in.read();
			}
			
			PrefixTree.SENTENCE_INITIAL_X = commandLine.getValue(sentence_initial_X);
			PrefixTree.SENTENCE_FINAL_X   = commandLine.getValue(sentence_final_X);
			
			PrefixTree.EDGE_X_MAY_VIOLATE_PHRASE_SPAN = commandLine.getValue(edge_X_violates);
			
			int lineNumber = 0;

			RuleExtractor ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, commandLine.getValue(ruleSampleSize), commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(minNonterminalSpan), commandLine.getValue(maxPhraseSpan));
			
			boolean oneTreePerSentence = ! commandLine.getValue(keepTree);
			
			PrefixTree prefixTree = null;
			while (testFileScanner.hasNextLine()) {
				
				Node.resetNodeCounter();
				
				String line = testFileScanner.nextLine();
				lineNumber++;
				
				int[] words = sourceVocab.getIDs(line);
				
				if (logger.isLoggable(Level.INFO)) logger.info("Constructing prefix tree for source line " + lineNumber + ": " + line);

				if (oneTreePerSentence || null==prefixTree) {
					prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(maxNonterminals), commandLine.getValue(minNonterminalSpan));
				}
				try {
					prefixTree.add(words);
				} catch (OutOfMemoryError e) {
					logger.warning("Out of memory - attempting to clear cache to free space");
					sourceSuffixArray.getCachedHierarchicalPhrases().clear();
					targetSuffixArray.getCachedHierarchicalPhrases().clear();
					prefixTree = null;
					System.gc();
					logger.info("Cleared cache and collected garbage. Now attempting to re-construct prefix tree...");
					prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(maxNonterminals), commandLine.getValue(minNonterminalSpan));
					prefixTree.add(words);
				}
				
				if (commandLine.getValue(print_prefixTree)==true) {
					System.out.println(prefixTree.toString());
				}
			
				if (commandLine.getValue(print_rules)) {
					if (logger.isLoggable(Level.FINE)) logger.fine("Outputting rules for source line: " + line);

					for (Rule rule : prefixTree.getAllRules()) {
						String ruleString = rule.toString(ntVocab, sourceVocab, targetVocab);
						if (logger.isLoggable(Level.FINEST)) logger.finest("Rule: " + ruleString);
						out.println(ruleString);
					}
				}
				
				if (logger.isLoggable(Level.FINER)) logger.finer(lexProbs.sizeInfo());
				
				if (commandLine.getValue(confirm)) {
					if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
					System.in.read();
					if (logger.isLoggable(Level.FINER)) {
						logger.finer("Prefix tree had " + prefixTree.size() + " nodes.");
						
						Cache<Pattern,MatchedHierarchicalPhrases> cache = sourceSuffixArray.getCachedHierarchicalPhrases();
						
						if (cache != null) {
							Pattern maxPattern = null;
							int maxHPsize = 0;
							int hpsize = 0;
							int psize = 0;
							for (Map.Entry<Pattern,MatchedHierarchicalPhrases> entry : cache.entrySet()) {
								psize++;
								hpsize += entry.getValue().size();
								if (hpsize>maxHPsize) {
									maxHPsize = entry.getValue().size();
									maxPattern = entry.getKey();
								}
							}

							logger.finer(
									psize + " source side entries in the SA cache." + "\n" +
									hpsize+ " target side HierarchicalPhrases represented in the cache." + "\n" +
									maxHPsize + " is the most HierarchicalPhrases stored for one source side entry ( " +
									maxPattern + ")\n"
							);	
						}
					}
					
				}
			}
			
		} catch (OutOfMemoryError e) {
			System.out.println("Node ID counter is at " + Node.nodeIDCounter);
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {

			if (finalConfirmation) {
				if (logger.isLoggable(Level.INFO)) logger.info("Complete: Please press a key to end program.");
				System.in.read();
			}
			
			if (logger.isLoggable(Level.INFO)) logger.info("Done extracting rules");
		}
	}

}
