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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
public class FrequentClassesTest {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(FrequentClassesTest.class.getName());
	
	
	String[] sentences = {
			"scientists complete sequencing of the chromosome linked to early dementia",
			"( afp , paris , january 2 ) an international team of scientists said that they have completed the sequencing of human chromosome 14 that is linked to many diseases , including the early-onset alzheimer's that may strike people in their 30s .",
			"this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .",
			"this study published in the weekly british scientific journal nature illustrates that the sequence of chromosome 14 comprises 1,050 genes and gene fragments .",
			"the goal of geneticists is to provide diagnostic tools to identify defective genes that cause diseases so as to arrive eventually at treatments that can prevent those genes from malfunctioning ."
	};

	String[] to_be_or_not_to_be = { "t o _ b e _ o r _ n o t _ t o _ b e" };
	Vocabulary symbolTableToBe;
	CorpusArray corpusToBe;
	SuffixArray suffixToBe;
	int[] knownToBeLCP;
	FrequencyClasses frequencyToBeClasses;
	FrequencyClass[] expectedClass;
	
	String corpusFileName;
	FrequentPhrases frequentPhrases;
	SuffixArray suffixArray;
	CorpusArray corpusArray;
	
	
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
			LinkedHashSet<String> set = new LinkedHashSet<String>();
			for (String sentence : to_be_or_not_to_be) {
				String[] array = sentence.split("\\s+");
				Arrays.sort(array);
				for (String s : array) { set.add(s); }
			}
			symbolTableToBe = new Vocabulary(set);
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, new Vocabulary(), true);

			logger.fine("Constructing corpus array from file " + corpusFileName);
			corpusToBe = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTableToBe, lengths[0], lengths[1]);

			logger.fine("Constructing suffix array from file " + corpusFileName);
			suffixToBe = new SuffixArray(corpusToBe, Cache.DEFAULT_CAPACITY);
			
			// Known suffixes are taken from Yamamoto and Church, figure 2
			int[] knownSuffixes = {15, 2, 8, 5, 12, 16, 3, 17, 4, 9, 14, 1, 6, 10, 7, 11, 13, 0};
			Assert.assertEquals(knownSuffixes.length, knownSuffixes.length);
			for (int i=0, n=knownSuffixes.length; i<n; i++) {
				Assert.assertEquals(suffixToBe.suffixes[i], knownSuffixes[i]);
			}
			
		
			knownToBeLCP = new int[]{0, 3, 1, 1, 1, 0, 2, 0, 1, 0, 0, 4, 1, 1, 0, 0, 1, 5, 0};
			int[] lcp = FrequentPhrases.calculateLongestCommonPrefixes(suffixToBe);
			
			Assert.assertNotNull(lcp);
			Assert.assertEquals(lcp.length, suffixToBe.size()+1);
			Assert.assertEquals(lcp.length, knownToBeLCP.length);
			for (int i=0, n=knownToBeLCP.length; i<n; i++) {
				Assert.assertEquals(lcp[i], knownToBeLCP[i]);
			}
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}

	}
	
	
	@Test(dependsOnMethods = {"longestCommonPrefix"})
	@SuppressWarnings("unused") 
	public void simpleFrequencyClassCount() {
		
		frequencyToBeClasses = FrequentPhrases.getFrequencyClasses(suffixToBe);
		
		Assert.assertNotNull(frequencyToBeClasses);
		Assert.assertEquals(frequencyToBeClasses.numTrivialClasses, 18);
		Assert.assertEquals(frequencyToBeClasses.numClasses, 8);
		Assert.assertEquals(frequencyToBeClasses.size(), 18+8);
		
		{
			int counter=0;
			for (FrequencyClass frequencyClass : frequencyToBeClasses) {
				counter++;
			}

			Assert.assertEquals(counter, frequencyToBeClasses.size());
		}
	}
	
	
	@Test(dependsOnMethods = {"longestCommonPrefix"})
	public void simpleFrequencyClass() {
		expectedClass = new FrequencyClass[] {
				new FrequencyClass(0, knownToBeLCP),
				new FrequencyClass(1, knownToBeLCP),
				new FrequencyClass(0, 1, 1, 2, knownToBeLCP),
				new FrequencyClass(2, knownToBeLCP),
				new FrequencyClass(3, knownToBeLCP),
				new FrequencyClass(4, knownToBeLCP),
				new FrequencyClass(0, 4, 2, 5, knownToBeLCP),
				new FrequencyClass(5, knownToBeLCP),
				new FrequencyClass(6, knownToBeLCP),
				new FrequencyClass(5, 6, 6, 2, knownToBeLCP),
				new FrequencyClass(7, knownToBeLCP),
				new FrequencyClass(8, knownToBeLCP),
				new FrequencyClass(7, 8, 8, 2, knownToBeLCP),
				new FrequencyClass(9, knownToBeLCP),
				new FrequencyClass(10, knownToBeLCP),
				new FrequencyClass(11, knownToBeLCP),
				new FrequencyClass(10, 11, 11, 2, knownToBeLCP),
				new FrequencyClass(12, knownToBeLCP),
				new FrequencyClass(13, knownToBeLCP),
				new FrequencyClass(10, 13, 12, 4, knownToBeLCP),
				new FrequencyClass(14, knownToBeLCP),
				new FrequencyClass(15, knownToBeLCP),
				new FrequencyClass(16, knownToBeLCP),
				new FrequencyClass(17, knownToBeLCP),
				new FrequencyClass(16, 17, 17, 2, knownToBeLCP),
				new FrequencyClass(15, 17, 16, 3, knownToBeLCP),
		};
		
		Assert.assertNotNull(expectedClass);
		
		Assert.assertEquals(expectedClass[0].getIntervalStart(), 0);
		Assert.assertEquals(expectedClass[0].getIntervalEnd(), 0);
		Assert.assertEquals(expectedClass[0].getFrequency(), 1);
		Assert.assertTrue(expectedClass[0].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[0].hasTrivialInterval());

		Assert.assertEquals(expectedClass[1].getIntervalStart(), 1);
		Assert.assertEquals(expectedClass[1].getIntervalEnd(), 1);
		Assert.assertEquals(expectedClass[1].getFrequency(), 1);
		Assert.assertTrue(expectedClass[1].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[1].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[2].getIntervalStart(), 0);
		Assert.assertEquals(expectedClass[2].getIntervalEnd(), 1);
		Assert.assertEquals(expectedClass[2].getFrequency(), 2);
		Assert.assertEquals(expectedClass[2].getRepresentativeIndex(), 1);
		Assert.assertFalse(expectedClass[2].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[3].getIntervalStart(), 2);
		Assert.assertEquals(expectedClass[3].getIntervalEnd(), 2);
		Assert.assertEquals(expectedClass[3].getFrequency(), 1);
		Assert.assertTrue(expectedClass[3].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[3].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[4].getIntervalStart(), 3);
		Assert.assertEquals(expectedClass[4].getIntervalEnd(), 3);
		Assert.assertEquals(expectedClass[4].getFrequency(), 1);
		Assert.assertTrue(expectedClass[4].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[4].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[5].getIntervalStart(), 4);
		Assert.assertEquals(expectedClass[5].getIntervalEnd(), 4);
		Assert.assertEquals(expectedClass[5].getFrequency(), 1);
		Assert.assertTrue(expectedClass[5].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[5].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[6].getIntervalStart(), 0);
		Assert.assertEquals(expectedClass[6].getIntervalEnd(), 4);
		Assert.assertEquals(expectedClass[6].getFrequency(), 5);
		Assert.assertEquals(expectedClass[6].getRepresentativeIndex(), 2);
		Assert.assertFalse(expectedClass[6].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[7].getIntervalStart(), 5);
		Assert.assertEquals(expectedClass[7].getIntervalEnd(), 5);
		Assert.assertEquals(expectedClass[7].getFrequency(), 1);
		Assert.assertTrue(expectedClass[7].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[7].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[8].getIntervalStart(), 6);
		Assert.assertEquals(expectedClass[8].getIntervalEnd(), 6);
		Assert.assertEquals(expectedClass[8].getFrequency(), 1);
		Assert.assertTrue(expectedClass[8].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[8].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[9].getIntervalStart(), 5);
		Assert.assertEquals(expectedClass[9].getIntervalEnd(), 6);
		Assert.assertEquals(expectedClass[9].getFrequency(), 2);
		Assert.assertEquals(expectedClass[9].getRepresentativeIndex(), 6);
		Assert.assertFalse(expectedClass[9].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[10].getIntervalStart(), 7);
		Assert.assertEquals(expectedClass[10].getIntervalEnd(), 7);
		Assert.assertEquals(expectedClass[10].getFrequency(), 1);
		Assert.assertTrue(expectedClass[10].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[10].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[11].getIntervalStart(), 8);
		Assert.assertEquals(expectedClass[11].getIntervalEnd(), 8);
		Assert.assertEquals(expectedClass[11].getFrequency(), 1);
		Assert.assertTrue(expectedClass[11].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[11].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[12].getIntervalStart(), 7);
		Assert.assertEquals(expectedClass[12].getIntervalEnd(), 8);
		Assert.assertEquals(expectedClass[12].getFrequency(), 2);
		Assert.assertEquals(expectedClass[12].getRepresentativeIndex(), 8);
		Assert.assertFalse(expectedClass[12].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[13].getIntervalStart(), 9);
		Assert.assertEquals(expectedClass[13].getIntervalEnd(), 9);
		Assert.assertEquals(expectedClass[13].getFrequency(), 1);
		Assert.assertTrue(expectedClass[13].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[13].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[14].getIntervalStart(), 10);
		Assert.assertEquals(expectedClass[14].getIntervalEnd(), 10);
		Assert.assertEquals(expectedClass[14].getFrequency(), 1);
		Assert.assertTrue(expectedClass[14].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[14].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[15].getIntervalStart(), 11);
		Assert.assertEquals(expectedClass[15].getIntervalEnd(), 11);
		Assert.assertEquals(expectedClass[15].getFrequency(), 1);
		Assert.assertTrue(expectedClass[15].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[15].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[16].getIntervalStart(), 10);
		Assert.assertEquals(expectedClass[16].getIntervalEnd(), 11);
		Assert.assertEquals(expectedClass[16].getFrequency(), 2);
		Assert.assertEquals(expectedClass[16].getRepresentativeIndex(), 11);
		Assert.assertFalse(expectedClass[16].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[17].getIntervalStart(), 12);
		Assert.assertEquals(expectedClass[17].getIntervalEnd(), 12);
		Assert.assertEquals(expectedClass[17].getFrequency(), 1);
		Assert.assertTrue(expectedClass[17].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[17].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[18].getIntervalStart(), 13);
		Assert.assertEquals(expectedClass[18].getIntervalEnd(), 13);
		Assert.assertEquals(expectedClass[18].getFrequency(), 1);
		Assert.assertTrue(expectedClass[18].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[18].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[19].getIntervalStart(), 10);
		Assert.assertEquals(expectedClass[19].getIntervalEnd(), 13);
		Assert.assertEquals(expectedClass[19].getFrequency(), 4);
		Assert.assertEquals(expectedClass[19].getRepresentativeIndex(), 12);
		Assert.assertFalse(expectedClass[19].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[20].getIntervalStart(), 14);
		Assert.assertEquals(expectedClass[20].getIntervalEnd(), 14);
		Assert.assertEquals(expectedClass[20].getFrequency(), 1);
		Assert.assertTrue(expectedClass[20].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[20].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[21].getIntervalStart(), 15);
		Assert.assertEquals(expectedClass[21].getIntervalEnd(), 15);
		Assert.assertEquals(expectedClass[21].getFrequency(), 1);
		Assert.assertTrue(expectedClass[21].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[21].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[22].getIntervalStart(), 16);
		Assert.assertEquals(expectedClass[22].getIntervalEnd(), 16);
		Assert.assertEquals(expectedClass[22].getFrequency(), 1);
		Assert.assertTrue(expectedClass[22].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[22].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[23].getIntervalStart(), 17);
		Assert.assertEquals(expectedClass[23].getIntervalEnd(), 17);
		Assert.assertEquals(expectedClass[23].getFrequency(), 1);
		Assert.assertTrue(expectedClass[23].getRepresentativeIndex() < 0);
		Assert.assertTrue(expectedClass[23].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[24].getIntervalStart(), 16);
		Assert.assertEquals(expectedClass[24].getIntervalEnd(), 17);
		Assert.assertEquals(expectedClass[24].getFrequency(), 2);
		Assert.assertEquals(expectedClass[24].getRepresentativeIndex(), 17);
		Assert.assertFalse(expectedClass[24].hasTrivialInterval());
		
		Assert.assertEquals(expectedClass[25].getIntervalStart(), 15);
		Assert.assertEquals(expectedClass[25].getIntervalEnd(), 17);
		Assert.assertEquals(expectedClass[25].getFrequency(), 3);
		Assert.assertEquals(expectedClass[25].getRepresentativeIndex(), 16);
		Assert.assertFalse(expectedClass[25].hasTrivialInterval());
	}
	
	@Test(dependsOnMethods = {"simpleFrequencyClassCount","simpleFrequencyClass"})
	public void simpleFrequencyClasses() {

		int index = 0;
		for (FrequencyClass frequencyClass : frequencyToBeClasses) {
			Assert.assertNotNull(frequencyClass);
			Assert.assertEquals(frequencyClass, expectedClass[index]);
			Assert.assertEquals(frequencyClass.hashCode(), expectedClass[index].hashCode());
			Assert.assertEquals(frequencyClass.hasTrivialInterval(), expectedClass[index].hasTrivialInterval());
			Assert.assertEquals(frequencyClass.getIntervalStart(), expectedClass[index].getIntervalStart());
			Assert.assertEquals(frequencyClass.getIntervalEnd(), expectedClass[index].getIntervalEnd());
			Assert.assertEquals(frequencyClass.getRepresentativeIndex(), expectedClass[index].getRepresentativeIndex());
			Assert.assertEquals(frequencyClass.getFrequency(), expectedClass[index].getFrequency());
			
			if (frequencyClass.hasTrivialInterval()) {
				Assert.assertEquals(frequencyClass.getIntervalStart(), frequencyClass.getIntervalEnd());
				Assert.assertEquals(frequencyClass.getFrequency(), 1);
				Assert.assertTrue(frequencyClass.getRepresentativeIndex() < 0);
			} else {
				Assert.assertFalse(frequencyClass.getIntervalStart()==frequencyClass.getIntervalEnd());
				Assert.assertTrue(frequencyClass.getFrequency() > 1);
				Assert.assertTrue(frequencyClass.getRepresentativeIndex() > 0);
			}
			
			index += 1;
		}


	}
	
	
	@Test(dependsOnMethods = {"simpleFrequencyClasses"})
	public void simplePhrases() {
		LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, Integer.MAX_VALUE, 1);
		
		Assert.assertNotNull(phrases);
		Assert.assertFalse(phrases.isEmpty());
		Assert.assertEquals(phrases.size(), 7);
		
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("r",symbolTableToBe)));
		Assert.assertTrue(phrases.containsKey(new BasicPhrase("n",symbolTableToBe)));

		Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("r",symbolTableToBe)), 1);
		Assert.assertEquals((int) phrases.get(new BasicPhrase("n",symbolTableToBe)), 1);
		
	}
	
	@Test(dependsOnMethods = {"simplePhrases"})
	public void simpleFrequentPhrases() {
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 1, Integer.MAX_VALUE, 1);
			
			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 7);
			
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("r",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("n",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("r",symbolTableToBe)), 1);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("n",symbolTableToBe)), 1);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 2, Integer.MAX_VALUE, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 5);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 3, Integer.MAX_VALUE, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 3);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 4, Integer.MAX_VALUE, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 2);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 5, Integer.MAX_VALUE, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 1);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 6, Integer.MAX_VALUE, 1);

			Assert.assertNotNull(phrases);
			Assert.assertTrue(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 0);

		}
	}
	
	@Test(dependsOnMethods = {"simplePhrases"})
	public void simpleFrequentPhrasesLimited() {
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 7, 1);
			
			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 7);
			
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("r",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("n",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("r",symbolTableToBe)), 1);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("n",symbolTableToBe)), 1);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 6, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 5);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
		}

		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 5, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 5);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("b",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("e",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("b",symbolTableToBe)), 2);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("e",symbolTableToBe)), 2);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 4, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 3);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 3, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 3);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("t",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("t",symbolTableToBe)), 3);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 2, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 2);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("o",symbolTableToBe)));
			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("o",symbolTableToBe)), 4);
			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 1, 1);

			Assert.assertNotNull(phrases);
			Assert.assertFalse(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 1);

			Assert.assertTrue(phrases.containsKey(new BasicPhrase("_",symbolTableToBe)));

			Assert.assertEquals((int) phrases.get(new BasicPhrase("_",symbolTableToBe)), 5);
		}
		
		{
			LinkedHashMap<Phrase,Integer> phrases = FrequentPhrases.getMostFrequentPhrases(suffixToBe, 0, 0, 1);

			Assert.assertNotNull(phrases);
			Assert.assertTrue(phrases.isEmpty());
			Assert.assertEquals(phrases.size(), 0);
		}
	}
	
	@Test(dependsOnMethods ={"simpleFrequentPhrases","simpleFrequentPhrasesLimited"})
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
			ArrayList<String> words = new ArrayList<String>();
			for (String sentence : sentences) {
				String[] array = sentence.split("\\s+");
				for (String s : array) { 
					if (! words.contains(s)) {
						words.add(s); 
					}
				}
			}
			Collections.sort(words);
			LinkedHashSet<String> set = new LinkedHashSet<String>(words);
			symbolTable = new Vocabulary(set);
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, new Vocabulary(), true);

			logger.fine("Constructing corpus array from file " + corpusFileName);
			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);

			logger.fine("Constructing suffix array from file " + corpusFileName);
			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
			
			int minFrequency = 0;
			short maxPhrases = 9;
			int maxPhraseLength = 1;
			int maxContiguousPhraseLength = 3;
			int maxPhraseSpan = Integer.MAX_VALUE;
			int minNonterminalSpan = 2;
			
			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
			frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
			
			Assert.assertNotNull(frequentPhrases);
			Assert.assertNotNull(frequentPhrases.frequentPhrases);
			
			Assert.assertEquals(frequentPhrases.maxPhrases, maxPhrases);
//			for (Map.Entry<Phrase, Integer> entry : frequentPhrases.frequentPhrases.entrySet()) { System.out.println(entry.getKey().toString() + " " + entry.getValue()); }
			Assert.assertEquals(frequentPhrases.frequentPhrases.size(), maxPhrases);
			
			Assert.assertNotNull(frequentPhrases.getSuffixes());
			Assert.assertEquals(frequentPhrases.getSuffixes(), frequentPhrases.suffixes);
			Assert.assertEquals(frequentPhrases.suffixes, suffixArray);
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}
	
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void frequencies() {
		
		LinkedHashMap<Phrase,Integer> map = frequentPhrases.frequentPhrases;
		Assert.assertNotNull(map);
		Assert.assertFalse(map.isEmpty());
		Assert.assertEquals(map.size(), 9);
		
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
			
		Assert.assertFalse(i.hasNext());
		
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void phrases() {
		
		LinkedHashMap<Phrase,Integer> map = frequentPhrases.frequentPhrases;
		Assert.assertNotNull(map);
		Assert.assertFalse(map.isEmpty());
		Assert.assertEquals(map.size(), 9);
		
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
		Assert.assertEquals(phrase.toString(), "of");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "to");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "that");
		
		
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
		Assert.assertEquals(phrase.toString(), "genes");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), ",");
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertEquals(phrase.toString(), "is");
		
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
		Assert.assertEquals(phrase.toString(), "of");
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
		Assert.assertEquals(phrase.toString(), "that");
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
		Assert.assertEquals(phrase.toString(), "genes");
		rank = entry.getValue();
		Assert.assertEquals(rank, 6);

		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), ",");
		rank = entry.getValue();
		Assert.assertEquals(rank, 7);
		
		Assert.assertTrue(i.hasNext());
		entry = i.next();
		phrase = entry.getKey();
		Assert.assertNotNull(phrase);
		Assert.assertEquals(phrase.toString(), "is");
		rank = entry.getValue();
		Assert.assertEquals(rank, 8);
		
		Assert.assertFalse(i.hasNext());
	}
	
	
	@Test(dependsOnMethods = {"simpleCollocationCount"})
	public void simpleCollocations() {
		short minNonterminalSpan = 0;

		int maxPhraseLength = 1;
		int maxContiguousPhraseLength = 3;
		short maxPhrases = Short.MAX_VALUE;
		int maxPhraseSpan = Integer.MAX_VALUE;
		
		{
			int minFrequency = 5;

			FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
			Assert.assertNotNull(frequentToBePhrases);

			List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
			int count = 0;
			for (HierarchicalPhrases phrases : list) {
				count += phrases.size();
			}
			Assert.assertFalse(count == 0);
			Assert.assertEquals(count, 10);

		}
		
	}
	
	
	@Test(dependsOnMethods = {"setup"})
	public void simpleCollocationCount() {

		
		
		{ // Use an essentially infinite window
			
			short minNonterminalSpan = 0;
			int maxContiguousPhraseLength = 1;
			int maxPhraseLength = Integer.MAX_VALUE;
			short maxPhrases = Short.MAX_VALUE;
			int maxPhraseSpan = Integer.MAX_VALUE;
			
			{
				int minFrequency = 6;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertTrue(count == 0);
				Assert.assertEquals(count, 0);

			}

			{
				int minFrequency = 5;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 10);

			}

			{
				int minFrequency = 4;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 36);

			}

			{
				int minFrequency = 3;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 66);

			}

			{
				int minFrequency = 2;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 120);

			}

			{
				int minFrequency = 1;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 153);

			}

			{
				int minFrequency = 0;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 153);

			}
		}
		
		
		
		{	// Use a minimal window
			
			short minNonterminalSpan = 1;
			int maxContiguousPhraseLength = 1;
			int maxPhraseLength = 3;
			short maxPhrases = Short.MAX_VALUE;
			int maxPhraseSpan = 3;
			
			{
				int minFrequency = 6;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertTrue(count == 0);
				Assert.assertEquals(count, 0);

			}
			
			{
				int minFrequency = 5;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertTrue(count == 0);
				Assert.assertEquals(count, 0);

			}
			
			{
				int minFrequency = 4;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 4);
//				Assert.assertEquals(count, 3);

			}
			
			{
				int minFrequency = 3;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count,7);
//				Assert.assertEquals(count, 8);

			}
			
			{
				int minFrequency = 2;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 13);

			}
			
			{
				int minFrequency = 1;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 16);
//				Assert.assertEquals(count, 17);

			}
			
			{
				int minFrequency = 0;

				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
				Assert.assertNotNull(frequentToBePhrases);

//				int count = frequentToBePhrases.getCollocations();
				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
				int count = 0;
				for (HierarchicalPhrases phrases : list) {
					count += phrases.size();
				}
				Assert.assertFalse(count == 0);
				Assert.assertEquals(count, 16);
//				Assert.assertEquals(count, 17);

			}
		}
		
//		{ // Use a reasonable window
//		  // The expected values were worked out by hand, painfully.
//			
//			short minNonterminalSpan = 0;
//			int maxContiguousPhraseLength = 1;
//			int maxPhraseLength = 3;
//			short maxPhrases = Short.MAX_VALUE;
//			int maxPhraseSpan = 5;
//			
//			{
//				// No phrase occurs 6 times
//				int minFrequency = 6;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertTrue(count == 0);
//				Assert.assertEquals(count, 0);
//
//			}
//			
//			{
//				// Two phrases occur at least 5 times: _ o
//				int minFrequency = 5;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 4);
//
//			}
//			
//			{
//				int minFrequency = 4;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 3+2+3+2+2+3+2+1);
//
//			}
//			
//			{
//				int minFrequency = 3;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 3+3+2 + 3+3 + 4 + 5 + 4+3+2+1);
//
//			}
//			
//			{
//				int minFrequency = 2;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 5+5+4+4+3+3+3+4+5+5+5+4+3+2+1);
//
//			}
//			
//			{
//				int minFrequency = 1;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 5+5+5+5+5+5+5+5+5+5+5+5+5+4+3+2+1);
//
//			}
//			
//			{
//				int minFrequency = 0;
//
//				FrequentPhrases frequentToBePhrases = new FrequentPhrases(suffixToBe, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//				Assert.assertNotNull(frequentToBePhrases);
//
////				int count = frequentToBePhrases.getCollocations();
//				List<HierarchicalPhrases> list = frequentToBePhrases.getFrequentCollocations();
//				int count = 0;
//				for (HierarchicalPhrases phrases : list) {
//					count += phrases.size();
//				}
//				Assert.assertFalse(count == 0);
//				Assert.assertEquals(count, 5+5+5+5+5+5+5+5+5+5+5+5+5+4+3+2+1);
//
//			}
//		
//		}
	}
	
	
	@Test(dependsOnMethods = {"setup"})
	public void collocationCount() {
		
		int minFrequency = 0;
		short maxPhrases = Short.MAX_VALUE;
		int maxPhraseLength = 3;
		int maxContiguousPhraseLength = 1;
		int maxPhraseSpan = 100;
		short minNonterminalSpan = 0;
		
		FrequentPhrases frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
		
		
//		int count = frequentPhrases.getCollocations();
		List<HierarchicalPhrases> list = frequentPhrases.getFrequentCollocations();
		int count = 0;
		for (HierarchicalPhrases phrases : list) {
			count += phrases.size();
		}
		Assert.assertFalse(count == 0);
		
	}
	
	@Test(dependsOnMethods = {"setup"})
	public void setupUnlimitedMaxPhrases() {
		
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
			short maxPhrases = Short.MAX_VALUE;
			int maxPhraseLength = 3;
			int maxContiguousPhraseLength = 1;
			int maxPhraseSpan = 1;
			int uniqueWords = 87;
			int minNonterminalSpan = 2;
			
			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
			FrequentPhrases frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
			
			Assert.assertNotNull(frequentPhrases);
			Assert.assertNotNull(frequentPhrases.frequentPhrases);
			
			Assert.assertEquals(frequentPhrases.maxPhrases, maxPhrases);
			
			Assert.assertEquals(frequentPhrases.frequentPhrases.size(), uniqueWords);
			
			Assert.assertNotNull(frequentPhrases.getSuffixes());
			Assert.assertEquals(frequentPhrases.getSuffixes(), frequentPhrases.suffixes);
			Assert.assertEquals(frequentPhrases.suffixes, suffixArray);
			
		} catch (IOException e) {
			Assert.fail("Unable to write temporary file. " + e.toString());
		}
	
	}
	
	
//	@Test(dependsOnMethods = {"setup"})
//	public void collocation() {
//		
//		// Tell System.out and System.err to use UTF8
//		FormatUtil.useUTF8();
//	
//		try {
//			
//			Vocabulary symbolTable;
//			Corpus corpusArray;
//			Suffixes suffixArray;
//			
//			logger.fine("Constructing vocabulary from file " + corpusFileName);
//			symbolTable = new Vocabulary();
//			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);
//
//			logger.fine("Constructing corpus array from file " + corpusFileName);
//			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);
//
//			logger.fine("Constructing suffix array from file " + corpusFileName);
//			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
//			
//			int minFrequency = 2;
//			short maxPhrases = 5;
//			int maxPhraseLength = 10;
//			int maxContiguousPhraseLength = 10;
//			int maxPhraseSpan = 10;
//			int minNonterminalSpan = 2;
//			
//			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
//			FrequentPhrases frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength, maxContiguousPhraseLength, maxPhraseSpan, minNonterminalSpan);
//			
//			// Get the most frequent contiguous phrases
//			frequentPhrases.cacheInvertedIndices();
//			Cache<Pattern,MatchedHierarchicalPhrases> cache = 
//				suffixArray.getCachedHierarchicalPhrases();
//			
//			List<HierarchicalPhrases> calculated = 
//				frequentPhrases.getFrequentCollocations();
//			
//			for 
//			
//		} catch (IOException e) {
//			Assert.fail("Unable to write temporary file. " + e.toString());
//		}
//	
//	}
	
	
//	@Test(dependsOnMethods = {"setup"})
//	public void setupUnlimitedMaxPhrasesLongPhrases() {
//		
//		// Tell System.out and System.err to use UTF8
//		FormatUtil.useUTF8();
//	
//		try {
//			
//			Vocabulary symbolTable;
//			Corpus corpusArray;
//			Suffixes suffixArray;
//			
//			logger.fine("Constructing vocabulary from file " + corpusFileName);
//			symbolTable = new Vocabulary();
//			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);
//
//			logger.fine("Constructing corpus array from file " + corpusFileName);
//			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);
//
//			logger.fine("Constructing suffix array from file " + corpusFileName);
//			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);
//			
//			int minFrequency = 0;
//			short maxPhrases = Short.MAX_VALUE;
//			int maxPhraseLength = 10;
//			int uniqueWords = 87;
//			
//			logger.fine("Calculating " + maxPhrases + " most frequent phrases");
//			FrequentPhrases frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength, maxPhraseSpan, minNonterminalSpan);
//			
//			Assert.assertNotNull(frequentPhrases);
//			Assert.assertNotNull(frequentPhrases.frequentPhrases);
//			
//			Assert.assertEquals(frequentPhrases.maxPhrases, maxPhrases);
//			
//			Assert.assertEquals(frequentPhrases.frequentPhrases.size(), uniqueWords);
//			
//			Assert.assertNotNull(frequentPhrases.getSuffixes());
//			Assert.assertEquals(frequentPhrases.getSuffixes(), frequentPhrases.suffixes);
//			Assert.assertEquals(frequentPhrases.suffixes, suffixArray);
//			
//		} catch (IOException e) {
//			Assert.fail("Unable to write temporary file. " + e.toString());
//		}
//	
//	}
}
