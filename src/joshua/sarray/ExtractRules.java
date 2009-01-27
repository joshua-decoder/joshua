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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import joshua.decoder.ff.tm.Rule;
import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;
import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.alignment.AlignmentGrids;
import joshua.util.sentence.alignment.Alignments;


/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
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
			Option<String> target = commandLine.addStringOption('e',"target","TARGET_FILE","Target language training file");		
			Option<String> alignment = commandLine.addStringOption('a',"alignments","ALIGNMENTS_FILE","Source-target alignments training file");	
			Option<String> test = commandLine.addStringOption('t',"test","TEST_FILE","Source language test file");

			Option<String> output = commandLine.addStringOption('o',"output","OUTPUT_FILE","-","Output file");


			Option<String> encoding = commandLine.addStringOption("encoding","ENCODING","UTF-8","File encoding format");

			Option<Integer> lexSampleSize = commandLine.addIntegerOption("lexSampleSize","LEX_SAMPLE_SIZE",1000, "Size to use when sampling for lexical probability calculations");
			Option<Integer> ruleSampleSize = commandLine.addIntegerOption("ruleSampleSize","RULE_SAMPLE_SIZE",300, "Maximum number of rules to store at each node in the prefix tree");

			Option<Integer> maxPhraseSpan = commandLine.addIntegerOption("maxPhraseSpan","MAX_PHRASE_SPAN",10, "Max phrase span");
			Option<Integer> maxPhraseLength = commandLine.addIntegerOption("maxPhraseLength","MAX_PHRASE_LENGTH",10, "Max phrase length");
			Option<Integer> maxNonterminals = commandLine.addIntegerOption("maxNonterminals","MAX_NONTERMINALS",2, "Max nonterminals");
			Option<Integer> minNonterminalSpan = commandLine.addIntegerOption("minNonterminalSpan","MIN_NONTERMINAL_SPAN", 2, "Minimum nonterminal span");
			
			Option<Integer> cacheSize = commandLine.addIntegerOption("cache","CACHE",1000, "Max number of patterns for which to cache hierarchical phrases");

			Option<Integer> trainingSize = commandLine.addIntegerOption("trainingSize","NUMBER_OF_TRAINING_SENTENCES",1000, "Expected number of training sentences");

			
//			Option<String> target_given_source_counts = commandLine.addStringOption("target-given-source-counts","FILENAME","file containing co-occurence counts of source and target word pairs, sorted by source words");
//			Option<String> source_given_target_counts = commandLine.addStringOption("source-given-target-counts","FILENAME","file containing co-occurence counts of target and source word pairs, sorted by target words");

			Option<Boolean> output_gz = commandLine.addBooleanOption("output-gzipped",false,"should the output file be gzipped");
//			Option<Boolean> target_given_source_gz = commandLine.addBooleanOption("target-given-source-gzipped",false,"is the target given source word pair counts file gzipped");
//			Option<Boolean> source_given_target_gz = commandLine.addBooleanOption("source-given-target-gzipped",false,"is the source given target word pair counts file gzipped");

			Option<Boolean> sentence_initial_X = commandLine.addBooleanOption("sentence-initial-X",false,"should rules with initial X be extracted from sentence-initial phrases");
			Option<Boolean> sentence_final_X = commandLine.addBooleanOption("sentence-final-X",false,"should rules with final X be extracted from sentence-final phrases");
			
			Option<Boolean> print_prefixTree = commandLine.addBooleanOption("print-prefix-tree",false,"should prefix tree be printed to standard out (for debugging)");
			Option<Boolean> print_rules = commandLine.addBooleanOption("print-rules",true,"should extracted rules be printed to standard out");
			
			Option<String> alignmentType = commandLine.addStringOption("alignmentsType","ALIGNMENT_TYPE","AlignmentGrids","Type of alignment data structure");
			Option<Boolean> confirm = commandLine.addBooleanOption("confirm",false,"should program pause for user input before constructing prefix trees?");
			
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

			SuffixArray.CACHE_CAPACITY = commandLine.getValue(cacheSize);
			if (logger.isLoggable(Level.INFO)) logger.info("Suffix array will cache hierarchical phrases for at most " + SuffixArray.CACHE_CAPACITY + " patterns.");
			
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language vocabulary.");
			String sourceFileName = commandLine.getValue(source);
			Vocabulary sourceVocab = new Vocabulary();
			int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language corpus array.");
			CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
			if (logger.isLoggable(Level.FINE)) logger.info("Constructing source language suffix array.");
			SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);

			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language vocabulary.");		
			String targetFileName = commandLine.getValue(target);
			Vocabulary targetVocab = new Vocabulary();
			int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(commandLine.getValue(target), targetVocab);
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language corpus array.");
			CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing target language suffix array.");
			SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);

			if (logger.isLoggable(Level.INFO)) logger.info("Reading alignment data.");
			String alignmentFileName = commandLine.getValue(alignment);
			Alignments alignments;
			if ("AlignmentArray".equals(commandLine.getValue(alignmentType))) {
				if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentArray");
				alignments = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);
			} else {
				if (logger.isLoggable(Level.INFO)) logger.info("Using AlignmentGrids");
				alignments = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, commandLine.getValue(trainingSize));
			}
			
			// Set up the source text for reading
//			Scanner target_given_source;
//			if (commandLine.getValue(target_given_source_counts).endsWith(".gz") || commandLine.getValue(target_given_source_gz))
//			target_given_source = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(target_given_source_counts))),commandLine.getValue(encoding))));
//			else
//			target_given_source = new Scanner( new File(commandLine.getValue(target_given_source_counts)), commandLine.getValue(encoding));


//			// Set up the target text for reading
//			Scanner source_given_target;
//			if (commandLine.getValue(source_given_target_counts).endsWith(".gz") || commandLine.getValue(source_given_target_gz))
//			source_given_target = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(source_given_target_counts))),commandLine.getValue(encoding))));
//			else
//			source_given_target = new Scanner( new File(commandLine.getValue(source_given_target_counts)), commandLine.getValue(encoding));

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
				new SampledLexProbs(commandLine.getValue(lexSampleSize), sourceSuffixArray, targetSuffixArray, alignments, false);
			//new LexProbs(source_given_target, target_given_source, sourceVocab, targetVocab);

			if (logger.isLoggable(Level.INFO)) logger.info("Done constructing lexical probabilities table");

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
			
			int lineNumber = 0;

			RuleExtractor ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, commandLine.getValue(ruleSampleSize), commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(minNonterminalSpan), commandLine.getValue(maxPhraseSpan));
			
			while (testFileScanner.hasNextLine()) {
				String line = testFileScanner.nextLine();
				lineNumber++;
				int[] words = sourceVocab.getIDs(line);

				if (logger.isLoggable(Level.INFO)) logger.info("Constructing prefix tree for source line " + lineNumber + ": " + line);

				PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, ruleExtractor, words, commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(maxNonterminals), commandLine.getValue(minNonterminalSpan));

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
				
				if (commandLine.getValue(confirm)) {
					if (logger.isLoggable(Level.INFO)) logger.info("Please press a key to continue");
					System.in.read();
					if (logger.isLoggable(Level.FINER)) logger.finer("Prefix tree had " + prefixTree.size() + " nodes.");
				}
			}
			
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
