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
package joshua.corpus.vocab;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;

import joshua.corpus.vocab.Vocabulary;


import org.testng.Assert;
import org.testng.annotations.Test;



/**
 *
 * 
 * @author Lane Schwartz
 */
public class VocabularyTest {

	/** [X], [X,1], [X,2], [S], [S,1] <unk>, <s>, </s>, -pau-*/
	int numBuiltInSymbols = 9;
	
	/** <unk>, <s>, </s>, -pau- */
	int numBuiltInTerminals = 4;
	
	@Test
	public void basicVocabTest() {
		
		Vocabulary vocab1 = new Vocabulary();
		Vocabulary vocab2 = new Vocabulary(new HashSet<String>());
		
		Assert.assertEquals(vocab1, vocab2);
		
		Assert.assertFalse(vocab1.intToString.isEmpty());
//		Assert.assertTrue(vocab1.intToString.get(0)==Vocabulary.UNKNOWN_WORD_STRING);
		Assert.assertFalse(vocab1.getWords().isEmpty());
		Assert.assertTrue(vocab1.getWord(0)==Vocabulary.UNKNOWN_WORD_STRING);
		Assert.assertEquals(vocab1.getWords(), vocab1.intToString.values());

		Assert.assertEquals(vocab1.size(), numBuiltInSymbols);
		Assert.assertEquals(vocab1.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

		//Assert.assertEquals(vocab1.getID("sample"), Vocabulary.UNKNOWN_WORD);
		//Assert.assertEquals(vocab1.getID(null), Vocabulary.UNKNOWN_WORD);

		Assert.assertFalse(vocab1.terminalToInt.isEmpty());
		Assert.assertEquals(vocab1.terminalToInt.size(), this.numBuiltInTerminals);
//		Assert.assertFalse(vocab1.isFixed);
//		
//		vocab1.fixVocabulary();
//		Assert.assertTrue(vocab1.isFixed);
		
		Assert.assertEquals(vocab1.getID(SymbolTable.X_STRING), -1);
		Assert.assertEquals(vocab1.getID(SymbolTable.X1_STRING), -2);
		Assert.assertEquals(vocab1.getID(SymbolTable.X2_STRING), -3);
		
		Assert.assertEquals(vocab1.getWord(-1), SymbolTable.X_STRING);
		Assert.assertEquals(vocab1.getWord(-2), SymbolTable.X1_STRING);
		Assert.assertEquals(vocab1.getWord(-3), SymbolTable.X2_STRING);
		
		
		
		Assert.assertFalse(vocab2.intToString.isEmpty());
//		Assert.assertTrue(vocab2.intToString.get(0)==Vocabulary.UNKNOWN_WORD_STRING);
		Assert.assertFalse(vocab2.getWords().isEmpty());
//		Assert.assertTrue(vocab2.getWord(0)==Vocabulary.UNKNOWN_WORD_STRING);
		Assert.assertEquals(vocab2.getWords(), vocab2.intToString.values());

		Assert.assertEquals(vocab2.size(), numBuiltInSymbols);
		Assert.assertEquals(vocab2.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

//		Assert.assertEquals(vocab2.getID("sample"), Vocabulary.UNKNOWN_WORD);
//		Assert.assertEquals(vocab2.getID(null), Vocabulary.UNKNOWN_WORD);
		
		Assert.assertFalse(vocab2.terminalToInt.isEmpty());
		Assert.assertEquals(vocab2.terminalToInt.size(), this.numBuiltInTerminals);
//		Assert.assertTrue(vocab2.isFixed);
		


	}

	@Test
	public void verifyWordIDs() throws IOException {
		
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
//		String queryString = "it persuades him and it disheartens him";
		
		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			sourcePrintStream.println(corpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
		
		Vocabulary vocab = new Vocabulary();
		Vocabulary.initializeVocabulary(sourceFileName, vocab, true);
		
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
		
//		Assert.assertEquals(vocab.getWord(vocab.getID("persuades")), Vocabulary.UNKNOWN_WORD_STRING);
//		Assert.assertEquals(vocab.getWord(vocab.getID("disheartens")), Vocabulary.UNKNOWN_WORD_STRING);
	}
	
	@Test
	public void loadVocabFromFile() {
		
		String filename = "data/tiny.en";
		int numSentences = 5;  // Should be 5 sentences in tiny.en
		int numWords = 89;     // Should be 89 words in tiny.en
		int numUniqWords = 60; // Should be 60 unique words in tiny.en
		
		Vocabulary vocab = new Vocabulary();
		Vocabulary vocab2 = new Vocabulary();
		
		Assert.assertTrue(vocab.equals(vocab2));
		Assert.assertTrue(vocab2.equals(vocab));
		Assert.assertEquals(vocab, vocab2);
		
		try {
			int[] result = Vocabulary.initializeVocabulary(filename, vocab, true);
			Assert.assertNotNull(result);
			Assert.assertEquals(result.length, 2);
			Assert.assertEquals(result[0], numWords); 
			Assert.assertEquals(result[1], numSentences);  
			
//			Assert.assertTrue(vocab.isFixed);
			Assert.assertEquals(vocab.size(), numUniqWords+numBuiltInSymbols);
			
		} catch (IOException e) {
			Assert.fail("Could not load file " + filename);
		}
		
		Assert.assertFalse(vocab.equals(vocab2));
		
		try {
			int[] result = Vocabulary.initializeVocabulary(filename, vocab2, true);
			Assert.assertNotNull(result);
			Assert.assertEquals(result.length, 2);
			Assert.assertEquals(result[0], numWords); 
			Assert.assertEquals(result[1], numSentences);  
			
//			Assert.assertTrue(vocab2.isFixed);
			Assert.assertEquals(vocab2.size(), numUniqWords+numBuiltInSymbols);
			
		} catch (IOException e) {
			Assert.fail("Could not load file " + filename);
		}
		
		Assert.assertEquals(vocab, vocab2);
	}
}
