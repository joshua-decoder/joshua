package joshua.decoder.ff.lm.srilm;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Scanner;

import joshua.corpus.SymbolTable;
import joshua.decoder.SrilmSymbol;
import joshua.util.io.BinaryOut;
import joshua.util.sentence.Vocabulary;



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
		SymbolTable symbolTable = new SrilmSymbol(null, lmOrder);
		new LMGrammarSRILM((SrilmSymbol)symbolTable, lmOrder, lmFile);
		
		
		// Write the map to a temporary file
		File tmpFile = File.createTempFile("srilm", "out");
		srilm.write_default_vocab_map(tmpFile.getAbsolutePath());
		
		
		// Create a vocabulary object from using the SRILM integer mappings
		Scanner scanner = new Scanner(tmpFile);
		Vocabulary vocab = Vocabulary.getVocabFromSRILM(scanner);
		vocab.fixVocabulary();
		
		
		// Write the vocabulary to disk in binary format
		ObjectOutput out = new BinaryOut(outVocabFile);
		vocab.writeExternal(out);
		
	}
	
}
