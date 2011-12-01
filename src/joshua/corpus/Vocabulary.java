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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.util.FormatUtils;
import joshua.util.MurmurHash;

/**
 * Static singular vocabulary class. Supports vocabulary freezing.
 * 
 * @author Juri Ganitkevitch
 */

public class Vocabulary {

	private static final Logger logger;

	private static KenLM kenlm;

	private static TreeMap<Long, Integer> hashToId;
	private static ArrayList<String> id_to_string;
	private static TreeMap<Long, String> hash_to_string;

	private static final Integer lock = new Integer(0);

	private static final int UNKNOWN_ID;
	private static final String UNKNOWN_WORD;

	static {
		logger = Logger.getLogger(Vocabulary.class.getName());

		UNKNOWN_ID = 0;
		UNKNOWN_WORD = "<unk>";
		
		kenlm = null;
		
		hashToId = new TreeMap<Long, Integer>();
		hash_to_string = new TreeMap<Long, String>();
		id_to_string = new ArrayList<String>();
		
		id_to_string.add(UNKNOWN_ID, UNKNOWN_WORD);
	}

	public static boolean registerLanguageModel(KenLM lm) {
		synchronized(lock) {
			kenlm = lm;
			boolean collision = false;
			for (int i = id_to_string.size() - 1; i > 0; i--)
				collision = collision || kenlm.registerWord(id_to_string.get(i), i);
			return collision;
		}
	}

	public static void read() {

	}

	public static void write() {

	}

	public static void freeze() {
		synchronized(lock) {
			int current_id = 1;
			Map.Entry<Long, Integer> walker = hashToId.firstEntry();
			while (walker != null) {
				if (walker.getValue() < 0)
					walker.setValue(-current_id);
				String word = hash_to_string.get(walker.getKey());
				id_to_string.add(current_id, word);
				current_id++;
				walker = hashToId.higherEntry(walker.getKey());
			}
		}
	}

	public static int id(String token) {
		synchronized(lock) {
			long hash = 0;
			try {
				hash = MurmurHash.hash64(token);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String hash_word = hash_to_string.get(hash);
			if (hash_word != null) {
				if (!token.equals(hash_word)) {
					logger.warning("MurmurHash for the following symbols collides: '"
								   + hash_word + "', '" + token + "'");
				}
				return hashToId.get(hash);
			} else {
				int id = id_to_string.size() * (nt(token) ? -1 : 1);
			
				if (kenlm != null) 
					kenlm.registerWord(token, Math.abs(id));
				id_to_string.add(token);
				hash_to_string.put(hash, token);
				hashToId.put(hash, id);
				return id;
			}
		}
	}

	public static int[] addAll(String sentence) {
		String[] tokens = sentence.split("\\s+");
		int[] ids = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++)
			ids[i] = id(tokens[i]);
		return ids;
	}

	public static String word(int id) {
		synchronized(lock) {
			id = Math.abs(id);
			if (id >= id_to_string.size())
				throw new UnknownSymbolException(id);
			return id_to_string.get(id);
		}
	}

	public static String getWords(int[] ids) {
		if (ids.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length - 1; i++)
			sb.append(word(ids[i])).append(" ");
		return sb.append(word(ids[ids.length - 1])).toString();
	}

	public static String getWords(Iterable<Integer> ids) {
		StringBuilder sb = new StringBuilder();
		for (int id : ids)
			sb.append(word(id)).append(" ");
		return sb.deleteCharAt(sb.length() - 1).toString();
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
	
	public static boolean idx(int id) {
		return (id < 0);
	}

	public static boolean nt(String word) {
		return FormatUtils.isNonterminal(word);
	}
	
	public static int size() {
		synchronized(lock) {
			return id_to_string.size();
		}
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

/**
 * Used to indicate that word hashing has produced a collision.
 * 
 * @author Juri Ganitkevitch
 * @version $LastChangedDate$
 */
class HashCollisionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HashCollisionException(String first, String second) {
		super("MurmurHash for the following symbols collides: '" + first + "', '" + second + "'");
	}
}
