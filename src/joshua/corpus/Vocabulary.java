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
package joshua.corpus;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Logger;

import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.util.FormatUtils;

/**
 * Static singular vocabulary class. Supports vocabulary freezing
 * 
 * @author Juri Ganitkevitch
 */

public class Vocabulary {

	private static final Logger logger; 

	private static KenLM kenlm;
	private static int[] lmMap;
	
	private static TreeMap<Long, Integer> hashToId;
	private static ArrayList<String> id_to_string;
	private static TreeMap<Long, String> hash_to_string;
	
	private static final int UNKNOWN_ID;
	private static final String UNKNOWN_WORD;
	
	private static boolean frozen;

	static {
		logger = Logger.getLogger(Vocabulary.class.getName());
		
		UNKNOWN_ID = 0;
		UNKNOWN_WORD = "<unk>";
		
		hashToId = new TreeMap<Long, Integer>();
		hash_to_string = new TreeMap<Long, String>();
		id_to_string = new ArrayList<String>();
	}
	
	public static boolean registerLanguageModel(KenLM lm) {
		return true;
	}
	
	public static void read() {
		
	}
	
	public static void write() {
		
	}
	
	public static boolean freeze() {
		// If there already frozen, we
		if (frozen) {
			logger.warning("Attempting to freeze a partially frozen vocabulary. " +
					"Aborted due to possible corruption. Use refreeze() to explicitly ");
			return false;
		}
		
		
		
		return true;
	}

	public static int id(String token) {
		return kenlm.vocabFindOrAdd(token);
	}

	public static int[] addAll(String sentence) {
		String[] tokens = sentence.split("\\s+");
		int[] ids = new int[tokens.length];
		for (int i = 0; i< tokens.length; i++)
			ids[i] = id(tokens[i]);
		return ids;
	}
	
	public static String word(int id) {
		return "";
	}
	
	public static String getWords(int[] ids) {
		if (ids.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length - 1; i++) 
			sb.append(word(ids[i])).append(" ");
		return sb.append(word(ids[ids.length - 1])).toString();
	}
		
	public static int getUnknownId() {
		return UNKNOWN_ID;
	}

	public static String getUnknownWord() {
		return UNKNOWN_WORD;
	}

	public static boolean nt(int id) {
		return (id < 0);
	}

	public static int size() {
		return id_to_string.size();
	}
	
	public static int getTargetNonterminalIndex(int id) {
		return FormatUtils.getNonterminalIndex(word(id));
	}
}

/**
 * Used to indicate that a query has been made for a symbol that is not known.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
class UnknownSymbolException extends RuntimeException {

	/** Serialization identifier. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an exception indicating that the specified identifier cannot be
	 * found in the symbol table.
	 * 
	 * @param id
	 *          Integer identifier
	 */
	public UnknownSymbolException(int id) {
		super("Identifier " + id + " cannot be found in the symbol table");
	}

	/**
	 * Constructs an exception indicating that the specified symbol cannot be
	 * found in the symbol table.
	 * 
	 * @param symbol
	 *          String symbol
	 */
	public UnknownSymbolException(String symbol) {
		super("Symbol " + symbol + " cannot be found in the symbol table");
	}
}
