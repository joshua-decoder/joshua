package joshua.corpus;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;

public class MemoryMappedSymbolTable extends AbstractSymbolTable {
//
//	private final IntBuffer binaryCorpusBuffer;
//	private final ByteBuffer binarySentenceBuffer;
//	
//	public MemoryMappedSymbolTable(String binaryFileName) {
//		
//	}
	
	public int addNonterminal(String nonterminal) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int addTerminal(String terminal) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Collection<Integer> getAllIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getHighestID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getID(String wordString) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int[] getIDs(String sentence) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLowestID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getTerminal(int wordId) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTerminals(int[] wordIDs) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUnknownWord() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getUnknownWordID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getWord(int tokenId) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getWords(int[] ids) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getWords() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isNonterminal(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
