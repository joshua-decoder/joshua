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
package edu.jhu.sa.util.suffix_array;


import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.jhu.sa.util.suffix_array.Pattern;
import edu.jhu.sa.util.suffix_array.PrefixTree;


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
	
	
}
