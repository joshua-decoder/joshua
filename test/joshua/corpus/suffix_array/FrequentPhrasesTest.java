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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.CorpusArray;
import joshua.corpus.Phrase;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.Cache;
import joshua.util.FormatUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for FrequentPhrases.
 *
 * @author Lane Schwartz
 */
public class FrequentPhrasesTest {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(FrequentPhrasesTest.class.getName());
	
	
	String[] sentences = {
			"scientists complete sequencing of the chromosome linked to early dementia",
			"( afp , paris , january 2 ) an international team of scientists said that they have completed the sequencing of human chromosome 14 that is linked to many diseases , including the early-onset alzheimer's that may strike people in their 30s .",
			"this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .",
			"this study published in the weekly british scientific journal nature illustrates that the sequence of chromosome 14 comprises 1,050 genes and gene fragments .",
			"the goal of geneticists is to provide diagnostic tools to identify defective genes that cause diseases so as to arrive eventually at treatments that can prevent those genes from malfunctioning ."
	};

	String[] to_be_or_not_to_be = { "t o _ b e _ o r _ n o t _ t o _ b e" };
	
	String corpusFileName;
	FrequentPhrases frequentPhrases;
	SuffixArray suffixArray;
	CorpusArray corpusArray;
	
	@Test
	public void setup() {
		
		// Tell System.out and System.err to use UTF8
		FormatUtil.useUTF8();
	
		try {
			
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			for (String sentence : sentences) {
				sourcePrintStream.println(sentence);
			}
			sourcePrintStream.close();
			corpusFileName = sourceFile.getAbsolutePath();
			
			Vocabulary symbolTable;
			
			logger.fine("Constructing vocabulary from file " + corpusFileName);
			symbolTable = new Vocabulary();
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);

			logger.fine("Constructing corpus array from file " + corpusFileName);
			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);

			logger.fine("Constructing suffix array from file " + corpusFileName);
			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
			
			int minFrequency = 0;
			short maxPhrases = 10;
			int maxPhraseLength = 1;

			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
			frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength);
			
			Assert.assertNotNull(frequentPhrases);
			Assert.assertNotNull(frequentPhrases.frequentPhrases);
			
			Assert.assertEquals(frequentPhrases.maxPhrases, maxPhrases);
			//for (Phrase phrase : frequentPhrases.frequentPhrases.keySet()) { System.out.println(phrase.toString()); }
			Assert.assertEquals(frequentPhrases.frequentPhrases.size(), maxPhrases);
			
			Assert.assertNotNull(frequentPhrases.getSuffixes());
			Assert.assertEquals(frequentPhrases.getSuffixes(), frequentPhrases.suffixes);
			Assert.assertEquals(frequentPhrases.suffixes, suffixArray);
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}
	
	}
	
	
	@Test
	public void longestCommonPrefix() {
		
		// Tell System.out and System.err to use UTF8
		FormatUtil.useUTF8();
	
		try {
			
			File sourceFile = File.createTempFile("to_be", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			for (String sentence : to_be_or_not_to_be) {
				sourcePrintStream.println(sentence);
			}
			sourcePrintStream.close();
			String corpusFileName = sourceFile.getAbsolutePath();
			
			logger.fine("Constructing vocabulary from file " + corpusFileName);
			Vocabulary symbolTable = new Vocabulary();
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);

			logger.fine("Constructing corpus array from file " + corpusFileName);
			Corpus corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);

			logger.fine("Constructing suffix array from file " + corpusFileName);
			SuffixArray suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
			
			// Known suffixes are taken from Yamamoto and Church, figure 2
			int[] knownSuffixes = {15, 2, 8, 5, 12, 16, 3, 17, 4, 9, 14, 1, 6, 10, 7, 11, 13, 0};
			Assert.assertEquals(knownSuffixes.length, knownSuffixes.length);
			for (int i=0, n=knownSuffixes.length; i<n; i++) {
				Assert.assertEquals(suffixArray.suffixes[i], knownSuffixes[i]);
			}
			
		
			int[] knownLCP = {0, 3, 1, 1, 1, 0, 2, 0, 1, 0, 0, 4, 1, 1, 0, 0, 1, 5, 0};
			int[] lcp = FrequentPhrases.calculateLongestCommonPrefixes(suffixArray);
			
			Assert.assertNotNull(lcp);
			Assert.assertEquals(lcp.length, suffixArray.size()+1);
			Assert.assertEquals(lcp.length, knownLCP.length);
			for (int i=0, n=knownLCP.length; i<n; i++) {
				Assert.assertEquals(lcp[i], knownLCP[i]);
			}
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}

	}
	
	
	@Test(dependsOnMethods = {"setup"})
	public void frequencies() {
		
		LinkedHashMap<Phrase,Integer> map = frequentPhrases.frequentPhrases;
		
		Iterator<Map.Entry<Phrase,Integer>> i = map.entrySet().iterator();
		Assert.assertNotNull(i);
		
		Map.Entry<Phrase, Integer> entry;
		int frequency;
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 7);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 6);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 6);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 6);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 5);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 4);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 3);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 3);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 3);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		frequency = entry.getValue();
		Assert.assertEquals(frequency, 2);
		
		Assert.assertFalse(i.hasNext());
		
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void phrases() {
		
		LinkedHashMap<Phrase,Integer> map = frequentPhrases.frequentPhrases;
		
		Iterator<Map.Entry<Phrase,Integer>> i = map.entrySet().iterator();
		Assert.assertNotNull(i);
		
		Map.Entry<Phrase, Integer> entry;
		Phrase phrase;
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "the");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "that");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "to");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "of");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), ".");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "chromosome");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "is");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "genes");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), ",");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "this");
		
		Assert.assertFalse(i.hasNext());
		
	}
	
	@Test(dependsOnMethods = {"setup","phrases"})
	public void phraseRanks() {
		
		LinkedHashMap<Phrase,Short> ranks = frequentPhrases.getRanks();
		Iterator<Map.Entry<Phrase,Short>> i = ranks.entrySet().iterator();
		Assert.assertNotNull(i);
		
		Map.Entry<Phrase,Short> entry;
		Phrase phrase;
		short rank;
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "the");
		rank = entry.getValue();
		Assert.assertEquals(rank, 0);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "that");
		rank = entry.getValue();
		Assert.assertEquals(rank, 1);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "to");
		rank = entry.getValue();
		Assert.assertEquals(rank, 2);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "of");
		rank = entry.getValue();
		Assert.assertEquals(rank, 3);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), ".");
		rank = entry.getValue();
		Assert.assertEquals(rank, 4);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "chromosome");
		rank = entry.getValue();
		Assert.assertEquals(rank, 5);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "is");
		rank = entry.getValue();
		Assert.assertEquals(rank, 6);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "genes");
		rank = entry.getValue();
		Assert.assertEquals(rank, 7);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), ",");
		rank = entry.getValue();
		Assert.assertEquals(rank, 8);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "this");
		rank = entry.getValue();
		Assert.assertEquals(rank, 9);
		
		Assert.assertFalse(i.hasNext());
	}
	
	
	@Test(dependsOnMethods = {"setup"})
	public void collocationCount() {
		
		int maxPhraseLength = 1;
		int windowSize = 100;
		
		int count = frequentPhrases.countCollocations(maxPhraseLength, windowSize);
		Assert.assertFalse(count == 0);
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void extendedSetup() {
		
		// Tell System.out and System.err to use UTF8
		FormatUtil.useUTF8();
	
		try {
			
			Vocabulary symbolTable;
			Corpus corpusArray;
			Suffixes suffixArray;
			
			logger.fine("Constructing vocabulary from file " + corpusFileName);
			symbolTable = new Vocabulary();
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);

			logger.fine("Constructing corpus array from file " + corpusFileName);
			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);

			logger.fine("Constructing suffix array from file " + corpusFileName);
			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
			
			int minFrequency = 0;
			short maxPhrases = 11;
			int maxPhraseLength = 1;
			int uniqueWords = 11;
			
			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
			FrequentPhrases frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength);
			
			Assert.assertNotNull(frequentPhrases);
			Assert.assertNotNull(frequentPhrases.frequentPhrases);
			
			Assert.assertEquals(frequentPhrases.maxPhrases, maxPhrases);
			
			for (Map.Entry<Phrase, Integer> entry : frequentPhrases.frequentPhrases.entrySet()) {
				Phrase phrase = entry.getKey();
				int count = entry.getValue();
				System.out.println(count + "\t" + phrase);
			}
			
			Assert.assertEquals(frequentPhrases.frequentPhrases.size(), uniqueWords);
			
			Assert.assertNotNull(frequentPhrases.getSuffixes());
			Assert.assertEquals(frequentPhrases.getSuffixes(), frequentPhrases.suffixes);
			Assert.assertEquals(frequentPhrases.suffixes, suffixArray);
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}
	
	}
}
