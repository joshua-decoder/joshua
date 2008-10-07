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
package joshua.decoder.ff.lm.bdb;

import static org.testng.AssertJUnit.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import joshua.decoder.ff.lm.bdb.SmoothedNGramDB.SmoothedNGram;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SmoothedNGramDBTest {

	private SmoothedNGramDB languageModel;
	
	@BeforeClass
	public void setUp() {
		
		try {
			String data = 
				"\n" +
				"\\data\\" + "\n" +
				"ngram 1=12" + "\n" +
				"ngram 2=4" + "\n" +
				"\n" +
				"\\1-grams:" + "\n" +
				"-3.433507       !       -2.168433" + "\n" +
				"-3.515715       \"       -0.1488718" + "\n" +
				"-5.146224       #       -1.926492" + "\n" +
				"-5.266363       $       -0.3721171" + "\n" +
				"-3.330871       %       -1.073414" + "\n" +
				"-4.933151       1996       -1.262715" + "\n" +
				"-4.933151       1997" + "\n" +
				"-1.83022	a	-1.241657" + "\n" +
				"-5.356173	boxes	-0.4249813" + "\n" +
				"-6.183985	boxing	-0.332314" + "\n" +
				"-5.706884	boy	-0.2148384" + "\n" +
				"-5.212568	boycott	-0.5567611" + "\n" +
				"\n" +
				"\\2-grams:" + "\n" +
				"-5.837146       a boxes" + "\n" +
				"-5.28867        a boxing" + "\n" +
				"-4.772416       a boy" + "\n" +
				"-4.281245       a boycott       -0.1924523" + "\n" +
				"\n" + 
				"\\end\\" + "\n";
			
			// Create a new unique temporary file
			File tmpDB = File.createTempFile("tmp", "db");

			// Delete the temporary file - we only needed it to get a new unique filename
			if (!tmpDB.delete()) fail("Unable to delete the temporary file");

			String dbDirectoryName = tmpDB.getAbsolutePath();
			String dbEncoding = "UTF-8";
			Scanner source = new Scanner(data);
			//Scanner source = new Scanner(new File("/project/nlp-models/lane/corpus/wmt06/training/tmp/LM"));
			
			languageModel = new SmoothedNGramDB(source, dbDirectoryName, dbEncoding);
			//languageModel = new SmoothedNGramDB("/project/nlp-models/lane/corpus/wmt06/training/ngrams", dbEncoding);
			
		} catch (IOException e) {
			fail("Unable to create required temporary file");
		}	
	}
	
	@Test
	public void verifyData() {
		SmoothedNGram ngram;

		ngram = languageModel.getSmoothedNGram("!");
		assertNotNull(ngram);
		assertEquals(Math.log(Math.pow(10.0, -3.433507)), ngram.getLogProb());
		assertEquals(Math.log(Math.pow(10.0, -2.168433)), ngram.getBackoff());

		ngram = languageModel.getSmoothedNGram("\"");
		assertNotNull(ngram);
		assertEquals(Math.log(Math.pow(10.0, -3.515715)), ngram.getLogProb());
		assertEquals(Math.log(Math.pow(10.0, -0.1488718)), ngram.getBackoff());
		


		// It is important to test that data is correct
		//    in cases where the ngram ends in a number and there is no backoff.
		//
		// In the past, there was a bug involving this case. that led to 

		
		ngram = languageModel.getSmoothedNGram("a");
		assertNotNull(ngram);
		assertEquals(Math.log(Math.pow(10.0, -1.83022)), ngram.getLogProb());
		assertEquals(Math.log(Math.pow(10.0, -1.241657)), ngram.getBackoff());
			
	}
	
	/**
	 * Test that ngram logprob and backoff data is correct for ngrams which end in a number.
	 * <p>
	 * In an earlier version of SmoothedNGramDB, there was a bug -
	 * if an ngram string ended in a number, and there was no backoff value for that ngram,
	 * the backoff was (incorrectly) interpreted to be the log of the number which ended the ngram.
	 * If this number was positive, the backoff value was stored as Double.POSITIVE_INFINITY.
	 * During decoding, backoffs are added with other probabilities. 
	 * It sometimes happened that Double.POSITIVE_INFINITY was added to Double.NEGATIVE_INFINITY, 
	 * resulting in a score of Double.NaN. This was bad.
	 */
	@Test
	public void verifyDataInvolvingNumbers() {
		
		SmoothedNGram ngram;
		
		ngram = languageModel.getSmoothedNGram("1997");
		assertNotNull(ngram);
		assertEquals(Math.log(Math.pow(10.0, -4.933151)), ngram.getLogProb());
		assertEquals(0.0, ngram.getBackoff());
		
		ngram = languageModel.getSmoothedNGram("1996");
		assertNotNull(ngram);
		assertEquals(Math.log(Math.pow(10.0, -4.933151)), ngram.getLogProb());
		assertEquals(Math.log(Math.pow(10.0, -1.262715)), ngram.getBackoff());
		
	}
	
	@Test
	public void getTest() {

		assertEquals(Math.log(Math.pow(10.0, -4.772416)), languageModel.get("a boy"));
		

		//double value = languageModel.get("or 1996 ,");
		//System.err.println(value);
		//System.err.println(languageModel.get("a boy"));
	}
	
}