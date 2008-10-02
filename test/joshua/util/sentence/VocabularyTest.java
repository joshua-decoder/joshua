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
package joshua.util.sentence;

import java.io.IOException;
import java.util.HashSet;

import joshua.util.sentence.Vocabulary;


import org.testng.Assert;
import org.testng.annotations.Test;

import edu.jhu.sa.util.suffix_array.BasicPhrase;
import edu.jhu.sa.util.suffix_array.SuffixArrayFactory;


/**
 *
 * 
 * @author Lane Schwartz
 */
public class VocabularyTest {

	@Test
	public void basicVocabTest() {
		
		Vocabulary vocab1 = new Vocabulary();
		Vocabulary vocab2 = new Vocabulary(new HashSet<String>());
		
		Assert.assertEquals(vocab1, vocab2);
		

		Assert.assertTrue(vocab1.vocabList.isEmpty());
		Assert.assertTrue(vocab1.getWords().isEmpty());
		Assert.assertEquals(vocab1.getWords(), vocab1.vocabList);

		Assert.assertEquals(vocab1.size(), 0);
		Assert.assertEquals(vocab1.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

		Assert.assertEquals(vocab1.getID("sample"), Vocabulary.UNKNOWN_WORD);

		try {
			vocab1.getID(null);
			Assert.fail("Expected to encountered NullPointerException, but did not");
		} catch (NullPointerException e) {}

		Assert.assertTrue(vocab1.wordToIDMap.isEmpty());
		Assert.assertFalse(vocab1.isFixed);

		vocab1.fixVocabulary();
		Assert.assertTrue(vocab1.isFixed);
		
		
		
		Assert.assertTrue(vocab2.vocabList.isEmpty());
		Assert.assertTrue(vocab2.getWords().isEmpty());
		Assert.assertEquals(vocab2.getWords(), vocab2.vocabList);

		Assert.assertEquals(vocab2.size(), 0);
		Assert.assertEquals(vocab2.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

		Assert.assertEquals(vocab2.getID("sample"), Vocabulary.UNKNOWN_WORD);

		try {
			vocab2.getID(null);
			Assert.fail("Expected to encountered NullPointerException, but did not");
		} catch (NullPointerException e) {}

		Assert.assertTrue(vocab2.wordToIDMap.isEmpty());
		Assert.assertTrue(vocab2.isFixed);

	}

	@Test
	public void verifyWordIDs() {
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
		String queryString = "it persuades him and it disheartens him";
		
		Vocabulary vocab = new Vocabulary();
		BasicPhrase corpusSentence = new BasicPhrase(corpusString, vocab);
		BasicPhrase querySentence = new BasicPhrase(queryString, vocab);
		vocab.fixVocabulary();
		vocab.alphabetize();
		
		Assert.assertEquals(vocab.getWord(vocab.getID("it")), "it");
		Assert.assertEquals(vocab.getWord(vocab.getID("makes")), "makes");
		Assert.assertEquals(vocab.getWord(vocab.getID("him")), "him");
		Assert.assertEquals(vocab.getWord(vocab.getID("and")), "and");
		Assert.assertEquals(vocab.getWord(vocab.getID("mars")), "mars");
		Assert.assertEquals(vocab.getWord(vocab.getID(",")), ",");
		Assert.assertEquals(vocab.getWord(vocab.getID("sets")), "sets");
		Assert.assertEquals(vocab.getWord(vocab.getID("on")), "on");
		Assert.assertEquals(vocab.getWord(vocab.getID("takes")), "takes");
		Assert.assertEquals(vocab.getWord(vocab.getID("off")), "off");
		
		Assert.assertEquals(vocab.getWord(vocab.getID("persuades")), "persuades");
		Assert.assertEquals(vocab.getWord(vocab.getID("disheartens")), "disheartens");
	}
	
	@Test
	public void loadVocabFromFile() {
		
		String filename = "data/tiny.en";
		int numSentences = 5;  // Should be 5 sentences in tiny.en
		int numWords = 89;     // Should be 89 words in tiny.en
		int numUniqWords = 60; // Should be 60 unique words in tiny.en
		
		Vocabulary vocab = new Vocabulary();
		Vocabulary vocab2 = new Vocabulary();
		
		Assert.assertEquals(vocab, vocab2);
		
		try {
			int[] result = SuffixArrayFactory.createVocabulary(filename, vocab);
			Assert.assertNotNull(result);
			Assert.assertEquals(result.length, 2);
			Assert.assertEquals(result[0], numWords); 
			Assert.assertEquals(result[1], numSentences);  
			
			Assert.assertTrue(vocab.isFixed);
			Assert.assertEquals(vocab.size(), numUniqWords);
			
		} catch (IOException e) {
			Assert.fail("Could not load file " + filename);
		}
		
		Assert.assertFalse(vocab.equals(vocab2));
		
		try {
			int[] result = SuffixArrayFactory.createVocabulary(filename, vocab2);
			Assert.assertNotNull(result);
			Assert.assertEquals(result.length, 2);
			Assert.assertEquals(result[0], numWords); 
			Assert.assertEquals(result[1], numSentences);  
			
			Assert.assertTrue(vocab2.isFixed);
			Assert.assertEquals(vocab2.size(), numUniqWords);
			
		} catch (IOException e) {
			Assert.fail("Could not load file " + filename);
		}
		
		Assert.assertEquals(vocab, vocab2);
	}
}
