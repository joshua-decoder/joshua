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
package joshua.corpus.suffix_array;


import joshua.corpus.Phrase;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.prefix_tree.PrefixTree;
import joshua.util.Cache;

import org.testng.Assert;
import org.testng.annotations.Test;



/**
 *
 * 
 * @author Lane Schwartz
 */
public class PatternTest {

	int[] words = {1, 2, 3, SymbolTable.X, 4, 5, 6, SymbolTable.X, 7};
	int[] extra = {30, 70, 22};
	int[] extendedWords = {1, 2, 3, SymbolTable.X, 4, 5, 6, SymbolTable.X, 7, 30, 70, 22};
	Vocabulary vocab = new Vocabulary();
	
	Pattern pattern;
	
	@Test
	public void basicPattern() {
		
		pattern = new Pattern(vocab, words);
		
		Assert.assertEquals(pattern.getWordIDs(), words);
		Assert.assertEquals(pattern.getVocab(), vocab);
		Assert.assertEquals(pattern.arity(), 2);
		
	}

	
	@Test(dependsOnMethods = {"basicPattern"})
	public void extendedPattern() {
		
		Pattern extendedPattern = new Pattern(pattern, extra);
		
		Assert.assertEquals(extendedPattern.getWordIDs().length, extendedWords.length);
		
		for (int i=0; i<extendedWords.length; i++) {
			Assert.assertEquals(extendedPattern.getWordIDs()[i], extendedWords[i]);
		}
		
		Assert.assertEquals(extendedPattern.getVocab(), vocab);
		Assert.assertEquals(extendedPattern.arity(), 2);
			
	}
	
	@Test(dependsOnMethods = {"basicPattern"})
	public void copiedPattern() {
		
		Phrase phrase = pattern;
		
		Pattern copiedPattern = new Pattern(phrase);
		
		Assert.assertEquals(copiedPattern.getWordIDs().length, words.length);
		for (int i=0; i<words.length; i++) {
			Assert.assertEquals(copiedPattern.getWordIDs()[i], words[i]);
		}
		Assert.assertEquals(copiedPattern.getVocab(), vocab);
		Assert.assertEquals(copiedPattern.arity(), 2);	
	}
	
	@Test
	public void getTerminalSequenceLengths() {
		
		{
			int[] flatWords = {1, 2, 3, 4, 5};
			Pattern flat = new Pattern(vocab, flatWords);
			byte[] flatSeqs = flat.getTerminalSequenceLengths();

			Assert.assertNotNull(flatSeqs);
			Assert.assertEquals(flatSeqs.length, 1);
			Assert.assertEquals(flatSeqs[0], 5);
		}
		
		{
			int[] hierWords = {1, SymbolTable.X, 6, 7};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {SymbolTable.X, 6, 7};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 1);
			Assert.assertEquals(hierSeqs[0], 2);
		}
		
		{
			int[] hierWords = {1, 2, 3, SymbolTable.X};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 1);
			Assert.assertEquals(hierSeqs[0], 3);
		}
		
		{
			int[] hierWords = {1, SymbolTable.X, 6, 7, SymbolTable.X};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {SymbolTable.X, 6, 7, SymbolTable.X, 10};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 2);
			Assert.assertEquals(hierSeqs[1], 1);
		}
		
		{
			int[] hierWords = {1, 2, 3, SymbolTable.X, 6, 7, SymbolTable.X};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 3);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {1, SymbolTable.X, 6, SymbolTable.X, 9};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 3);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 1);
			Assert.assertEquals(hierSeqs[2], 1);
		}
		
		{
			int[] hierWords = {1, 2, 3, SymbolTable.X, 6, 7, SymbolTable.X, 9, 10, 11, 12};
			Pattern hier = new Pattern(vocab, hierWords);
			byte[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 3);
			Assert.assertEquals(hierSeqs[0], 3);
			Assert.assertEquals(hierSeqs[1], 2);
			Assert.assertEquals(hierSeqs[2], 4);
		}
	}
	
	@Test(dependsOnMethods = {"extendedPattern","copiedPattern"})
	public void cacheTest() {
		
		Cache<Pattern,Integer> cache = new Cache<Pattern,Integer>(10);
		
		cache.put(pattern, 1);
		
		Assert.assertTrue(cache.containsKey(pattern));
		Assert.assertEquals((int) cache.get(pattern), 1);
		
		Pattern copiedPattern = new Pattern(pattern);
		Assert.assertTrue(cache.containsKey(copiedPattern));
		Assert.assertEquals((int) cache.get(copiedPattern), 1);
	}
	
}
