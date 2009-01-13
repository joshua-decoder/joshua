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


import joshua.sarray.Pattern;
import joshua.sarray.PrefixTree;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;



/**
 *
 * 
 * @author Lane Schwartz
 */
public class PatternTest {

	int[] words = {1, 2, 3, PrefixTree.X, 4, 5, 6, PrefixTree.X, 7};
	int[] extra = {30, 70, 22};
	int[] extendedWords = {1, 2, 3, PrefixTree.X, 4, 5, 6, PrefixTree.X, 7, 30, 70, 22};
	Vocabulary vocab = new Vocabulary();
	
	Pattern pattern;
	
	@Test
	public void basicPattern() {
		
		pattern = new Pattern(vocab, words);
		
		Assert.assertEquals(pattern.getWords(), words);
		Assert.assertEquals(pattern.getVocab(), vocab);
		Assert.assertEquals(pattern.arity(), 2);
		
	}

	
	@Test(dependsOnMethods = {"basicPattern"})
	public void extendedPattern() {
		
		Pattern extendedPattern = new Pattern(pattern, extra);
		
		Assert.assertEquals(extendedPattern.getWords().length, extendedWords.length);
		
		for (int i=0; i<extendedWords.length; i++) {
			Assert.assertEquals(extendedPattern.getWords()[i], extendedWords[i]);
		}
		
		Assert.assertEquals(extendedPattern.getVocab(), vocab);
		Assert.assertEquals(extendedPattern.arity(), 2);
			
	}
	
	@Test(dependsOnMethods = {"basicPattern"})
	public void copiedPattern() {
		
		Phrase phrase = pattern;
		
		Pattern copiedPattern = new Pattern(phrase);
		
		Assert.assertEquals(copiedPattern.getWords().length, words.length);
		for (int i=0; i<words.length; i++) {
			Assert.assertEquals(copiedPattern.getWords()[i], words[i]);
		}
		Assert.assertEquals(copiedPattern.getVocab(), vocab);
		Assert.assertEquals(copiedPattern.arity(), 2);	
	}
	
	@Test
	public void getTerminalSequenceLengths() {
		
		{
			int[] flatWords = {1, 2, 3, 4, 5};
			Pattern flat = new Pattern(vocab, flatWords);
			int[] flatSeqs = flat.getTerminalSequenceLengths();

			Assert.assertNotNull(flatSeqs);
			Assert.assertEquals(flatSeqs.length, 1);
			Assert.assertEquals(flatSeqs[0], 5);
		}
		
		{
			int[] hierWords = {1, PrefixTree.X, 6, 7};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {PrefixTree.X, 6, 7};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 1);
			Assert.assertEquals(hierSeqs[0], 2);
		}
		
		{
			int[] hierWords = {1, 2, 3, PrefixTree.X};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 1);
			Assert.assertEquals(hierSeqs[0], 3);
		}
		
		{
			int[] hierWords = {1, PrefixTree.X, 6, 7, PrefixTree.X};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {PrefixTree.X, 6, 7, PrefixTree.X, 10};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 2);
			Assert.assertEquals(hierSeqs[1], 1);
		}
		
		{
			int[] hierWords = {1, 2, 3, PrefixTree.X, 6, 7, PrefixTree.X};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 2);
			Assert.assertEquals(hierSeqs[0], 3);
			Assert.assertEquals(hierSeqs[1], 2);
		}
		
		{
			int[] hierWords = {1, PrefixTree.X, 6, PrefixTree.X, 9};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 3);
			Assert.assertEquals(hierSeqs[0], 1);
			Assert.assertEquals(hierSeqs[1], 1);
			Assert.assertEquals(hierSeqs[2], 1);
		}
		
		{
			int[] hierWords = {1, 2, 3, PrefixTree.X, 6, 7, PrefixTree.X, 9, 10, 11, 12};
			Pattern hier = new Pattern(vocab, hierWords);
			int[] hierSeqs = hier.getTerminalSequenceLengths();
			
			Assert.assertNotNull(hierSeqs);
			Assert.assertEquals(hierSeqs.length, 3);
			Assert.assertEquals(hierSeqs[0], 3);
			Assert.assertEquals(hierSeqs[1], 2);
			Assert.assertEquals(hierSeqs[2], 4);
		}
	}
	
}
