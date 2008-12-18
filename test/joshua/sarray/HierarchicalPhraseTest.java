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
package joshua.sarray;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class HierarchicalPhraseTest {

	CorpusArray sourceCorpusArray;
	
	@Test
	public void setup() throws IOException {

//		String alignmentString = 
//			"0-0 0-1 1-1 2-1 3-1 0-2 0-3 5-4 4-5 6-5 8-6 8-7 7-8 10-9 12-10 11-11 12-11 13-12 14-13 15-13 16-13 16-14 17-15 18-16 19-17 19-18 19-19 19-20 19-21 20-22 21-24 22-24 25-29 24-31 26-32 27-33 28-34 30-35 31-36 29-37 30-37 31-37 31-38 32-39" + "\n" +
//			"0-0 0-1 0-2 1-3 2-5 3-6 4-6 5-7 6-8 7-9 8-10 10-11 12-11 9-12 11-12 12-12 13-13 14-14 18-16 21-17 22-19 22-20 23-20 24-21 25-22 25-23 26-24 27-25 28-25 29-26 30-26 31-26 31-28 32-29 34-30 33-31 35-33 36-34 36-35 37-36" + "\n" +
//			"0-0 1-0 2-1 3-2 4-3 5-4 6-5 7-6 8-7 9-11 10-12 11-13 12-14 10-15 11-15 12-15 13-16 14-17 15-17 16-17 19-17 18-18 21-19 22-20" + "\n";

		String sourceCorpusString = 
			"declaro reanudado el período de sesiones del parlamento europeo , interrumpido el viernes 17 de diciembre pasado , y reitero a sus señorías mi deseo de que hayan tenido unas buenas vacaciones ." + "\n" + 
			"como todos han podido comprobar , el gran `` efecto del año 2000 '' no se ha producido . en cambio , los ciudadanos de varios de nuestros países han sido víctimas de catástrofes naturales verdaderamente terribles ." + "\n" +
			"sus señorías han solicitado un debate sobre el tema para los próximos días , en el curso de este período de sesiones ." + "\n";

		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}

		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		
	}
	
	
	@Test
	public void testHasAlignedTerminal() {
		{

			Vocabulary vocab = sourceCorpusArray.getVocabulary();
			
			{
				Pattern     pattern = new Pattern(vocab, vocab.getIDs("de sesiones del parlamento europeo"));
				int[]       terminalSequenceStartIndices = {4};
				int[]       terminalSequenceEndIndices = {9};
				int         length = 5;

				HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

				Assert.assertFalse(phrase.containsTerminalAt(0));
				Assert.assertFalse(phrase.containsTerminalAt(1));
				Assert.assertFalse(phrase.containsTerminalAt(2));
				Assert.assertFalse(phrase.containsTerminalAt(3));

				Assert.assertTrue(phrase.containsTerminalAt(4));
				Assert.assertTrue(phrase.containsTerminalAt(5));
				Assert.assertTrue(phrase.containsTerminalAt(6));
				Assert.assertTrue(phrase.containsTerminalAt(7));
				Assert.assertTrue(phrase.containsTerminalAt(8));

				Assert.assertFalse(phrase.containsTerminalAt(9));
				Assert.assertFalse(phrase.containsTerminalAt(10));
				Assert.assertFalse(phrase.containsTerminalAt(11));

				Assert.assertFalse(phrase.containsTerminalAt(Integer.MAX_VALUE));
				Assert.assertFalse(phrase.containsTerminalAt(-1));
			}
			
			{
				Pattern     pattern = new Pattern(vocab, vocab.getIDs(","));
				int[]       terminalSequenceStartIndices = {9};
				int[]       terminalSequenceEndIndices = {10};
				int         length = 1;

				HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

				Assert.assertFalse(phrase.containsTerminalAt(0));
				Assert.assertFalse(phrase.containsTerminalAt(1));
				Assert.assertFalse(phrase.containsTerminalAt(2));
				Assert.assertFalse(phrase.containsTerminalAt(3));

				Assert.assertFalse(phrase.containsTerminalAt(4));
				Assert.assertFalse(phrase.containsTerminalAt(5));
				Assert.assertFalse(phrase.containsTerminalAt(6));
				Assert.assertFalse(phrase.containsTerminalAt(7));
				Assert.assertFalse(phrase.containsTerminalAt(8));

				Assert.assertTrue(phrase.containsTerminalAt(9));
				Assert.assertFalse(phrase.containsTerminalAt(10));
				Assert.assertFalse(phrase.containsTerminalAt(11));

				Assert.assertFalse(phrase.containsTerminalAt(Integer.MAX_VALUE));
				Assert.assertFalse(phrase.containsTerminalAt(-1));
			}
			
		}
	}
	
}
