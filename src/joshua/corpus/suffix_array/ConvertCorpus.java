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

import java.io.IOException;
import java.io.ObjectInput;
import java.util.logging.Logger;

import joshua.corpus.CorpusArray;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.io.BinaryIn;

/**
 * Given a corpus and an existing symbol table, read the corpus,
 * and create a binary representation of the corpus using the
 * provided symbol table.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class ConvertCorpus {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(ConvertCorpus.class.getName());
	
	/**
	 * Given a corpus and an existing symbol table, read the
	 * corpus, and create a binary representation of the corpus
	 * using the provided symbol table.
	 *
	 * @param args Command line arguments
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		// Read the command line arguments
		if (args.length < 3) {
			System.err.println(
					"Usage: java " + SuffixArray.class.getName() + 
					" target_corpus tgt.lm.vocab tgt.corpus");
			System.exit(-1);
		}
		String corpusFileName = args[0];
		String binaryVocabFilename = args[1];
		String binaryCorpusFilename = args[2];
		String charset = (args.length > 3) ? args[3] : "UTF-8";
		
		// Read the provided symbol table
		logger.info("Reading provided symbol table");
		Vocabulary symbolTable = new Vocabulary();
		ObjectInput in = BinaryIn.vocabulary(binaryVocabFilename);
		symbolTable.readExternal(in);
		
		// Read the provided corpus
		logger.info("Reading provided corpus");
		Vocabulary oldSymbolTable = new Vocabulary();
		int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, oldSymbolTable, true);
		CorpusArray corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, oldSymbolTable, lengths[0], lengths[1]);
		
		// Change the internal integer-string mappings
		// of the corpus to use those provided by the given symbol table.
		logger.info("Converting corpus to use new symbol mappings");
		corpusArray.setSymbolTable(symbolTable);
		
		// Write the corpus to disk in binary format
		logger.info("Writing corpus to disk in binary format, using new symbol mappings");
		corpusArray.write(binaryCorpusFilename, binaryVocabFilename, charset);
		
	}

}
