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


import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.vocab.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * @author Lane Schwartz
 */
public class AbstractHierarchicalPhrasesTest {

	@Test
	public void equalityTest() {
	
		Vocabulary vocab = new Vocabulary();
		vocab.addNonterminal("X");
		vocab.addTerminal("en");
		vocab.addTerminal("de");
		int X = vocab.getNonterminalID("X");
		int en = vocab.getID("en");
		int de = vocab.getID("de");
		
		int[] M_a_alpha_startPositions = {25,30,27,30};
		int[] M_a_alpha_sentenceNumbers = {2,2};
		Pattern M_a_alpha_pattern = new Pattern(vocab, en, X, de, X);
		Assert.assertEquals(M_a_alpha_pattern.arity(),2);
		
		MatchedHierarchicalPhrases a = 
			new HierarchicalPhrases(M_a_alpha_pattern, M_a_alpha_startPositions, M_a_alpha_sentenceNumbers);
		
		MatchedHierarchicalPhrases b = 
			new HierarchicalPhrases(M_a_alpha_pattern, M_a_alpha_startPositions, M_a_alpha_sentenceNumbers);
		
		MatchedHierarchicalPhrases c = 
			new HierarchicalPhrases(M_a_alpha_pattern, M_a_alpha_startPositions, M_a_alpha_sentenceNumbers);
		
		
		Assert.assertTrue(a.equals(a));
		Assert.assertTrue(b.equals(b));
		Assert.assertTrue(c.equals(c));
		
		Assert.assertFalse(a==b);
		Assert.assertFalse(a==c);
		Assert.assertFalse(b==a);
		Assert.assertFalse(b==c);
		
		Assert.assertTrue(a.equals(b));
		Assert.assertTrue(a.equals(c));
		Assert.assertTrue(b.equals(a));
		Assert.assertTrue(b.equals(c));
		Assert.assertTrue(c.equals(a));
		Assert.assertTrue(c.equals(b));
		
		
		int[] M_alpha_b_startPositions = {2,5,30,33,30,36,700,703,952,956};
		int[] M_alpha_b_sentenceNumbers = {1,2,2,57,94};
		Pattern M_alpha_b_pattern = new Pattern(vocab, X, de, X, en);
		Assert.assertEquals(M_alpha_b_pattern.arity(),2);
		MatchedHierarchicalPhrases d = 
			new HierarchicalPhrases(M_alpha_b_pattern, M_alpha_b_startPositions, M_alpha_b_sentenceNumbers);
		
		Assert.assertFalse(a.equals(d));
		Assert.assertFalse(b.equals(d));
		Assert.assertFalse(c.equals(d));
		
		Assert.assertFalse(d.equals(a));
		Assert.assertFalse(d.equals(b));
		Assert.assertFalse(d.equals(c));
	}
	
	@Test
	public void queryIntersectTest() {
		
		Vocabulary vocab = new Vocabulary();
		vocab.addNonterminal("X");
		vocab.addTerminal("en");
		vocab.addTerminal("de");
		int X = vocab.getNonterminalID("X");
		int en = vocab.getID("en");
		int de = vocab.getID("de");
		
		int[] M_a_alpha_startPositions = {25,30,27,30};
		int[] M_a_alpha_sentenceNumbers = {2,2};
		Pattern M_a_alpha_pattern = new Pattern(vocab, en, X, de, X);
		Assert.assertEquals(M_a_alpha_pattern.arity(),2);
		MatchedHierarchicalPhrases M_a_alpha = 
			new HierarchicalPhrases(M_a_alpha_pattern, M_a_alpha_startPositions, M_a_alpha_sentenceNumbers);
		Assert.assertEquals(M_a_alpha.size(),2);
		Assert.assertEquals(M_a_alpha.arity(),2);
		Assert.assertEquals(M_a_alpha.getNumberOfTerminalSequences(),2);
		Assert.assertFalse(M_a_alpha.startsWithNonterminal());
		Assert.assertFalse(M_a_alpha.secondTokenIsTerminal());
		Assert.assertTrue(M_a_alpha.endsWithNonterminal());
		Assert.assertFalse(M_a_alpha.endsWithTwoTerminals());
		Assert.assertFalse(M_a_alpha.isEmpty());
		Assert.assertEquals(M_a_alpha.getFirstTerminalIndex(0), 25);
		Assert.assertEquals(M_a_alpha.getFirstTerminalIndex(1), 27);
		Assert.assertEquals(M_a_alpha.getLastTerminalIndex(0), 30+1);
		Assert.assertEquals(M_a_alpha.getLastTerminalIndex(1), 30+1);
		
		
		int[] M_alpha_b_startPositions = {2,5,30,33,30,36,700,703,952,956};
		int[] M_alpha_b_sentenceNumbers = {1,2,2,57,94};
		Pattern M_alpha_b_pattern = new Pattern(vocab, X, de, X, en);
		Assert.assertEquals(M_alpha_b_pattern.arity(),2);
		MatchedHierarchicalPhrases M_alpha_b = 
			new HierarchicalPhrases(M_alpha_b_pattern, M_alpha_b_startPositions, M_alpha_b_sentenceNumbers);
		Assert.assertEquals(M_alpha_b.size(), 5);
		Assert.assertEquals(M_alpha_b.arity(),2);
		Assert.assertEquals(M_alpha_b.getNumberOfTerminalSequences(),2);
		Assert.assertTrue(M_alpha_b.startsWithNonterminal());
		Assert.assertTrue(M_alpha_b.secondTokenIsTerminal());
		Assert.assertFalse(M_alpha_b.endsWithNonterminal());
		Assert.assertFalse(M_alpha_b.endsWithTwoTerminals());
		Assert.assertFalse(M_alpha_b.isEmpty());
		Assert.assertEquals(M_alpha_b.getFirstTerminalIndex(0), 2);
		Assert.assertEquals(M_alpha_b.getFirstTerminalIndex(1), 30);
		Assert.assertEquals(M_alpha_b.getFirstTerminalIndex(2), 30);
		Assert.assertEquals(M_alpha_b.getFirstTerminalIndex(3), 700);
		Assert.assertEquals(M_alpha_b.getFirstTerminalIndex(4), 952);
		Assert.assertEquals(M_alpha_b.getLastTerminalIndex(0), 5+1);
		Assert.assertEquals(M_alpha_b.getLastTerminalIndex(1), 33+1);
		Assert.assertEquals(M_alpha_b.getLastTerminalIndex(2), 36+1);
		Assert.assertEquals(M_alpha_b.getLastTerminalIndex(3), 703+1);
		Assert.assertEquals(M_alpha_b.getLastTerminalIndex(4), 956+1);
		
		int minNonterminalSpan = 2;
		int maxPhraseSpan = 10;
		
		MatchedHierarchicalPhrases M_a_alpha_b =
			AbstractHierarchicalPhrases.queryIntersect(new Pattern(vocab, en, X, de, X, en), M_a_alpha, M_alpha_b, minNonterminalSpan, maxPhraseSpan, null);
	
		Assert.assertNotNull(M_a_alpha_b);
		Assert.assertEquals(M_a_alpha_b.size(), 3);
		Assert.assertEquals(M_a_alpha_b.arity(),2);
		Assert.assertEquals(M_a_alpha_b.getNumberOfTerminalSequences(),3);
		Assert.assertFalse(M_a_alpha_b.isEmpty());
		Assert.assertEquals(M_a_alpha_b.getFirstTerminalIndex(0), 25);
		Assert.assertEquals(M_a_alpha_b.getFirstTerminalIndex(1), 27);
		Assert.assertEquals(M_a_alpha_b.getFirstTerminalIndex(2), 27);
		Assert.assertEquals(M_a_alpha_b.getLastTerminalIndex(0), 33+1);
		Assert.assertEquals(M_a_alpha_b.getLastTerminalIndex(1), 33+1);
		Assert.assertEquals(M_a_alpha_b.getLastTerminalIndex(2), 36+1);
		
		Assert.assertFalse(M_a_alpha_b.startsWithNonterminal());
		Assert.assertFalse(M_a_alpha_b.secondTokenIsTerminal());
		Assert.assertFalse(M_a_alpha_b.endsWithNonterminal());
		Assert.assertFalse(M_a_alpha_b.endsWithTwoTerminals());
		
	}
}
