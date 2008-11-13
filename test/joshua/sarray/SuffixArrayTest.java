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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import joshua.sarray.BasicPhrase;
import joshua.sarray.ContiguousPhrase;
import joshua.sarray.CorpusArray;
import joshua.sarray.SuffixArray;
import joshua.sarray.SuffixArray.Collocations;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;


import org.testng.Assert;
import org.testng.annotations.Test;



public class SuffixArrayTest {

	private final SuffixArray suffixArray;
	private final Vocabulary vocab;
	
	public SuffixArrayTest() {
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";

		vocab = new Vocabulary();
		Phrase exampleSentence = new BasicPhrase(corpusString, vocab);
		vocab.alphabetize();
		vocab.fixVocabulary();
		
		exampleSentence = new BasicPhrase(corpusString, vocab);
		int[] sentences = new int[1];
		sentences[0] = 0;
		int[] corpus = new int[exampleSentence.size()];
		for(int i = 0; i < exampleSentence.size(); i++) {
			corpus[i] = exampleSentence.getWordID(i);
		}
		
		CorpusArray corpusArray = new CorpusArray(corpus, sentences, vocab);
		suffixArray = new SuffixArray(corpusArray);
		
	}
	
	@Test
	public void findPhrase() {
		
		// Look up phrase "it makes him"
		
		Phrase phrase = new BasicPhrase("it makes him", vocab);
		int[] bounds = suffixArray.findPhrase(phrase);
		
		int expectedSuffixArrayStartIndex = 8;
		int expectedSuffixArrayEndIndex = 8;
		
		Assert.assertEquals(bounds.length, 2);
		Assert.assertEquals(bounds[0], expectedSuffixArrayStartIndex);
		Assert.assertEquals(bounds[1], expectedSuffixArrayEndIndex);
		
		
		// Look up phrase "and it"
		
		phrase = new BasicPhrase("and it", vocab);
		bounds = suffixArray.findPhrase(phrase);
		
		expectedSuffixArrayStartIndex = 2;
		expectedSuffixArrayEndIndex = 3;
		
		Assert.assertEquals(bounds.length, 2);
		Assert.assertEquals(bounds[0], expectedSuffixArrayStartIndex);
		Assert.assertEquals(bounds[1], expectedSuffixArrayEndIndex);
	}
	
	//@Test
	public void print() {
		
		int[] lcpArray = suffixArray.calculateLongestCommonPrefixes();
		
		System.out.println("I\tS[I]\tLCP\tSUFFIX");
		for(int i = 0; i < suffixArray.size(); i++) {
			Phrase phrase = new ContiguousPhrase(suffixArray.suffixes[i], suffixArray.size(), suffixArray.corpus);
			System.out.println(i + "\t" + suffixArray.suffixes[i] + "\t" + lcpArray[i] + "\t"+ phrase);
		}
		System.out.println();
		
		//ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		ArrayList<Integer> frequencies = new ArrayList<Integer>();
		int minFrequency = 1;
		int maxPhrasesToRetain = 100;
		int maxPhraseLength = 100;
		List<Phrase> phrases = suffixArray.getMostFrequentPhrases(frequencies, minFrequency, maxPhrasesToRetain, maxPhraseLength);
		
		System.out.println("Frequency\tphrase");
		for(int i = 0; i < phrases.size(); i++) {
			System.out.println(frequencies.get(i) + "\t" + phrases.get(i));
		}
		System.out.println();
		
		
		System.out.println("Collocations");
		Collocations collocations = suffixArray.getCollocations(new HashSet<Phrase>(phrases), maxPhraseLength, 100);
		System.out.println(collocations);	
		
		Phrase phrase1 = new BasicPhrase("him", vocab);
		Phrase phrase2 = new BasicPhrase("it", vocab);
		
		int[] positions1 = suffixArray.getAllPositions(suffixArray.findPhrase(phrase1));
		int[] positions2 = suffixArray.getAllPositions(suffixArray.findPhrase(phrase2));
		
		System.out.print(phrase1 + " occurred at positions: ");
		for(int i = 0; i < positions1.length; i++) {
			System.out.print(positions1[i] + " ");
		}
		System.out.println();
		
		System.out.print(phrase2 + " occurred at positions: ");
		for(int i = 0; i < positions2.length; i++) {
			System.out.print(positions2[i] + " ");
		}
		System.out.println();

	}
}
