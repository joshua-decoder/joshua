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
package joshua.decoder.ff.lm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.lm.buildin_lm.TrieLM;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for testing ARPA language model class.
 * 
 * @author Lane Schwartz
 */
public class ArpaFileTest {

	String arpaFileName;
	SymbolTable vocab;
	
	@Test
	public void setup() {
		
		vocab = new Vocabulary();
		vocab.addTerminal("a");
		vocab.addTerminal("because");
		vocab.addTerminal("boycott");
		vocab.addTerminal("of");
		vocab.addTerminal("parliament");
		vocab.addTerminal("potato");
		vocab.addTerminal("resumption");
		vocab.addTerminal("the");
		
		try {
			File file = File.createTempFile("testLM", "arpa");
			PrintStream out = new PrintStream(file, "UTF-8");
			
			out.println();
			out.println("\\data\\");
			out.println("ngram 1=8");
			out.println("ngram 2=4");
			out.println("ngram 3=1");
			out.println();
			
			out.println("\\1-grams:");
			out.println("-1.992672       a       -0.1195484");
			out.println("-2.713723       because -0.4665429");
			out.println("-4.678545       boycott -0.0902521");
			out.println("-1.609573       of      -0.1991907");
			out.println("-3.875917       parliament      -0.1274891");
			out.println("-9.753210       potato");
			out.println("-4.678545       resumption      -0.07945678");
			out.println("-1.712444       the     -0.1606644");
			
			out.println();
			out.println("\\2-grams:");
			out.println("-0.3552987      because of      -0.03083654");
			out.println("-1.403534       of a");
			out.println("-0.7507797      of the  -0.05237135");
			out.println("-0.7266324      resumption of");
			out.println("-3.936147       the resumption");
			
			out.println();
			out.println("\\3-grams:");
			out.println("-0.6309999      because of the");
			out.println();
			
			out.println("\\end\\");
			
			out.close();
			this.arpaFileName = file.getAbsolutePath();
			
		} catch (IOException e) {
			Assert.fail("Unable to create temporary file: " + e.toString());
		}
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testOrder() {
		ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);
		
		try {
			Assert.assertEquals(arpaFile.getOrder(), 3);
		} catch (FileNotFoundException e) {
			Assert.fail(e.toString());
		}
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testIteration() {
		
		ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);
		
		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		
		boolean iterationOccurred = false;
		
		for (ArpaNgram ngram : arpaFile) {
		
			iterationOccurred = true;
			
			int order = ngram.order();
//			System.err.println("Order = " + order);
			
			int count;
			if (counts.containsKey(order)) {
				count = counts.get(order) + 1;
			} else {
				count = 1;
			}
			
			counts.put(order, count);

		}
		
		Assert.assertTrue(iterationOccurred);

		Assert.assertTrue(counts.containsKey(1));
		Assert.assertTrue(counts.containsKey(2));
		Assert.assertTrue(counts.containsKey(3));
		
		Assert.assertEquals((int) counts.get(1), 8);
		Assert.assertEquals((int) counts.get(2), 5);
		Assert.assertEquals((int) counts.get(3), 1);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testSize() {
		ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);
		
		Assert.assertEquals(arpaFile.size(), 14);
	}
	
	@Test(dependsOnMethods={"setup","testIteration"})
	public void testChildren() throws FileNotFoundException {
		ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);
		
		TrieLM lm = new TrieLM(arpaFile);
//		System.err.println(lm.getChildren().size());
		Assert.assertNotSame(lm.getChildren().size(), 0);
	}
	
	@Test(dependsOnMethods={"setup","testIteration","testChildren"})
	public void testTrie() throws FileNotFoundException {
		ArpaFile arpaFile = new ArpaFile(arpaFileName, vocab);
		
		TrieLM lm = new TrieLM(arpaFile);

		// Test unigrams known to be in the language model
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("a")),-1.992672, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("because")),-2.713723, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("boycott")),-4.678545, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("of")),-1.609573, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("parliament")),-3.875917, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("potato")),-9.753210, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("resumption")),-4.678545, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("the")),-1.712444, 0.000001f);
		
		// Test unigrams known to NOT be in the language model
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("banana")), -JoshuaConfiguration.lm_ceiling_cost, 0.000001f);
		
		
		// Test bigrams known to be in the language model
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("because of")), -0.3552987, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("of the")), -0.7507797, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("resumption of")), -0.7266324, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("the resumption")), -3.936147, 0.000001f);
		
		// Test trigrams known to be in the language model
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("because of the")), -0.6309999f, 0.000001f);
	
	
		// Test bigrams know to NOT be in the language model (but the unigrams are)
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("a boycott")), -4.678545f + -0.1195484f, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("of parliament")), -3.875917f + -0.1991907f, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("the potato")), -9.753210f + -0.1606644f, 0.000001f);
		Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("potato parliament")), -3.875917f + -0.0f, 0.000001f);
		
		// Test trigrams know to NOT be in the language model (but the bigrams are)
		int[] words = vocab.getIDs("because of a");
		double f = lm.ngramLogProbability(words);
		Assert.assertEquals(f, -1.403534f + -0.03083654f, 0.000001f);
//		//Assert.assertEquals(lm.ngramLogProbability(vocab.getIDs("of the parliament")), -3.875917f + -0.05237135f, 0.000001f);
		
	}
}
