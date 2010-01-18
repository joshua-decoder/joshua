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
package joshua.prefix_tree;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.AlignmentGrids;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.suffix_array.AbstractHierarchicalPhrases;
import joshua.corpus.suffix_array.HierarchicalPhrases;
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.util.FormatUtil;

/**
 *
 *
 * @author Lane Schwartz
 */
public class ExtractRuleProfiler {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(ExtractRuleProfiler.class.getName());
	
	public static void main(String[] args) throws IOException {

		// Tell System.out and System.err to use UTF8
		FormatUtil.useUTF8();

		logger.info("Starting up - current count is " + AbstractHierarchicalPhrases.counter);
		
		
		int trainingLines = 1000;
		
		String sourceCorpusString = 
			"it makes him and it mars him , it sets him on yet it takes him off .";
		
		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			for (int i=0; i<trainingLines; i++) {
				sourcePrintStream.println(sourceCorpusString);	
			}
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
	
		String targetCorpusString = 
			"das macht ihn und es besch\u00E4digt ihn , es setzt ihn auf und es f\u00FChrt ihn aus .";
		
		
		String targetFileName;
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintWriter targetPrintStream = new PrintWriter(targetFile, "UTF-8");
//			PrintStream targetPrintStream = new PrintStream(targetFile, "UTF-8");
			for (int i=0; i<trainingLines; i++) {
				targetPrintStream.println(targetCorpusString);
			}
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}
		
		String alignmentString = 
			"0-0 1-1 2-2 3-3 4-4 5-5 6-6 7-7 8-8 9-9 10-10 11-11 12-12 13-13 14-14 15-15 16-16 17-17";
		
		String alignmentFileName;
		{
			File alignmentFile = File.createTempFile("alignment", new Date().toString());
			PrintStream alignmentPrintStream = new PrintStream(alignmentFile);
			for (int i=0; i<trainingLines; i++) {
				alignmentPrintStream.println(alignmentString);
			}
			alignmentPrintStream.close();
			alignmentFileName = alignmentFile.getAbsolutePath();
		}

		//String alignmentsType = alignmentsType;
	
		int maxCacheSize = 100000;//12566;
		
		int numSourceWords, numSourceSentences;
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = Vocabulary.initializeVocabulary(sourceFileName, sourceVocab, true);
		numSourceWords = sourceWordsSentences[0];
		numSourceSentences = sourceWordsSentences[1];
		
		Corpus sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, numSourceWords, numSourceSentences);
		Suffixes sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, maxCacheSize);
		
		int numTargetWords, numTargetSentences;
		Vocabulary targetVocab = new Vocabulary();
		int[] targetWordsSentences = Vocabulary.initializeVocabulary(targetFileName, targetVocab, true);
		numTargetWords = targetWordsSentences[0];
		numTargetSentences = targetWordsSentences[1];
		
		Corpus targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, numTargetWords, numTargetSentences);
		Suffixes targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, maxCacheSize);
		
		int trainingSize = sourceCorpusArray.getNumSentences();
		boolean requireTightSpans = true;
		Alignments alignments = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, trainingSize, requireTightSpans);
		
//		ParallelCorpus parallelCorpus = 
//			new AlignedParallelCorpus(sourceCorpusArray, targetCorpusArray, alignments);
		
//		LexicalProbabilities lexProbs = 
//			new LexProbs(parallelCorpus, Float.MIN_VALUE);
		
		Map<Integer,String> ntVocab = new HashMap<Integer,String>();
		ntVocab.put(PrefixTree.X, "X");
		
		int ruleSampleSize = 300;
		int maxPhraseSpan = 10;
		int maxPhraseLength = 10;
		int minNonterminalSpan = 2;
		int maxNonterminals = 2;
		
//		RuleExtractor ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, ruleSampleSize, maxPhraseSpan, maxPhraseLength, minNonterminalSpan, maxPhraseSpan);
		
		int[] words = sourceVocab.getIDs(sourceCorpusString);
		
		int numIterations = 5;
		long[] times = new long[numIterations];
		
		for (int i=0; i<numIterations; i++) {
			logger.info("Extracting rules for sentence " + (i+1) + ".");
			long startTime1 = System.currentTimeMillis();
			{
				ParallelCorpusGrammarFactory parallelCorpus = new ParallelCorpusGrammarFactory(sourceSuffixArray, targetSuffixArray, alignments, null, ruleSampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan, Float.MIN_VALUE, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.default_non_terminal, JoshuaConfiguration.oovFeatureCost);

//				PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
				PrefixTree prefixTree = new PrefixTree(parallelCorpus);
				
				prefixTree.sentenceInitialX = true;
				prefixTree.sentenceFinalX   = true;
				prefixTree.edgeXMayViolatePhraseSpan = true;
				prefixTree.add(words);
			}
			long endTime1 = System.currentTimeMillis();
			logger.info("Cached HPs: " + sourceSuffixArray.getCachedHierarchicalPhrases().size());
			logger.info("Current count is " + AbstractHierarchicalPhrases.counter);
			logger.info("HP Constructor counts: " + HierarchicalPhrases.publicCounter + ", " + HierarchicalPhrases.protectedCounter + "," + HierarchicalPhrases.privateCounter + "," + HierarchicalPhrases.emptyListCounter);

			times[i] = endTime1 - startTime1;
		}
		
		for (long time : times) {
			logger.info("Time == " + time);
		}
		
//		logger.info("Extracting rules for second sentence.");
//		long startTime2 = System.currentTimeMillis();
//		{
//			PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, sourceSuffixArray.getVocabulary(), lexProbs, ruleExtractor, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
//			prefixTree.add(words);
//		}
//		long endTime2 = System.currentTimeMillis();
//		logger.info("Cached HPs: " + sourceSuffixArray.getCachedHierarchicalPhrases().size());
//		logger.info("Current count is " + AbstractHierarchicalPhrases.counter);
//		logger.info("HP Constructor counts: " + HierarchicalPhrases.publicCounter + ", " + HierarchicalPhrases.protectedCounter + "," + HierarchicalPhrases.privateCounter + "," + HierarchicalPhrases.emptyListCounter);
//		
//		long time1 = endTime1 - startTime1;
//		long time2 = endTime2 - startTime2;
//		
//		logger.info("Time1 == " + time1);
//		logger.info("Time2 == " + time2);
		
//		Assert.assertTrue(time2 < time1);
	}
}
