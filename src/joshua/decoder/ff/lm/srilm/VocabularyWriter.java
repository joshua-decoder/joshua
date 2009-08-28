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
package joshua.decoder.ff.lm.srilm;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Scanner;

import joshua.corpus.vocab.SrilmSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.io.BinaryOut;


/**
 * Converts an SRILM language model file to a Joshua-style binary
 * vocabulary file.
 * <p>
 * TODO The logic required to export SrilmSymbol should be put in
 * a <code>SrilmSymbol#writeExternal(ObjectOutput)</code> method.
 */
public class VocabularyWriter {
	
	public static void main(String[] args) throws IOException {
		
		// Read command line options
		if (args.length != 2) {
			System.err.println("Usage: java " + VocabularyWriter.class.getSimpleName() + " lmFile outBinaryVocabFile");
			System.exit(1);
		}		
		String lmFile = args[0].trim(); 
		String outVocabFile = args[1].trim();
		 
		
		// Load the lm file so that the SRI toolkit will set up the map
		int lmOrder = 1;
		SymbolTable symbolTable = new SrilmSymbol(lmOrder);
		new LMGrammarSRILM((SrilmSymbol)symbolTable, lmOrder, lmFile);
		
		
		// Write the map to a temporary file
		File tmpFile = File.createTempFile("srilm", "out");
		srilm.write_default_vocab_map(tmpFile.getAbsolutePath());
		
		
		// Create a vocabulary object from using the SRILM integer mappings
		Scanner scanner = new Scanner(tmpFile);
		Vocabulary vocab = Vocabulary.getVocabFromSRILM(scanner);
//		vocab.fixVocabulary();
		
		
		// Write the vocabulary to disk in binary format
		ObjectOutput out = new BinaryOut(outVocabFile);
		vocab.writeExternal(out);
		
	}
	
}
