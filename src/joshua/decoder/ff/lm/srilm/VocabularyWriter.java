package joshua.decoder.ff.lm.srilm;

import java.io.IOException;

import joshua.corpus.SymbolTable;
import joshua.decoder.SrilmSymbol;



public class VocabularyWriter {
	
	public static void main(String[] args) throws IOException {
		//=== command options
		if (args.length != 3) {
			System.out.println("wrong command, correct command should be: java VocabularyWriter lmFile lmOrder outVocabFile");
			System.exit(1);
		}		
		String lmFile = args[0].trim();
		int lmOrder = new Integer(args[1].trim());
		String outVocabFile = args[2].trim();
		
		
		//=== load the lm file so that the SRI toolkit will set up the map
		/**Lane: the loading of a hugh LM might be slow, to speed up, you may just try to set lmOrder=1, regardless of the true order of the lmFile; 
		 * But, you should verify if the table is exactly the same as if you have load the full-order LM
		 **/  
		SymbolTable symbolTable = new SrilmSymbol(null, lmOrder);
		LMGrammarSRILM lmGrammar  = new LMGrammarSRILM((SrilmSymbol)symbolTable, lmOrder, lmFile);
		
		
		//=== write the map
		srilm.write_default_vocab_map(outVocabFile);
	}
	
}
