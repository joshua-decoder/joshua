package joshua.corpus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.util.FormatUtils;
import joshua.util.MurmurHash;

/**
 * Static singular vocabulary class. Supports vocabulary freezing and (de-)serialization into a
 * vocabulary file.
 * 
 * @author Juri Ganitkevitch
 */

public class Vocabulary {

  private static final Logger logger;

  private static ArrayList<NGramLanguageModel> lms;

  private static TreeMap<Long, Integer> hashToId;
  private static ArrayList<String> idToString;
  private static TreeMap<Long, String> hashToString;

  private static final Integer lock = new Integer(0);

  private static final int UNKNOWN_ID;
  private static final String UNKNOWN_WORD;

  public static final String START_SYM = "<s>";
  public static final String STOP_SYM = "</s>";
  
  static {
    logger = Logger.getLogger(Vocabulary.class.getName());

    UNKNOWN_ID = 0;
    UNKNOWN_WORD = "<unk>";

    lms = new ArrayList<NGramLanguageModel>();

    clear();
  }

  public static boolean registerLanguageModel(NGramLanguageModel lm) {
    synchronized (lock) {
      // Store the language model.
      lms.add(lm);
      // Notify it of all the existing words.
      boolean collision = false;
      for (int i = idToString.size() - 1; i > 0; i--)
        collision = collision || lm.registerWord(idToString.get(i), i);
      return collision;
    }
  }

  /**
   * Reads a vocabulary from file. This deletes any additions to the vocabulary made prior to
   * reading the file.
   * 
   * @param file_name
   * @return Returns true if vocabulary was read without mismatches or collisions.
   * @throws IOException
   */
  public static boolean read(String file_name) throws IOException {
    synchronized (lock) {
      File vocab_file = new File(file_name);
      DataInputStream vocab_stream =
          new DataInputStream(new BufferedInputStream(new FileInputStream(vocab_file)));
      int size = vocab_stream.readInt();
      logger.info("Reading vocabulary: " + size + " tokens.");
      clear();
      for (int i = 0; i < size; i++) {
        int id = vocab_stream.readInt();
        String token = vocab_stream.readUTF();
        if (id != Math.abs(id(token))) {
          vocab_stream.close();
          return false;
        }
      }
      vocab_stream.close();
      return (size + 1 == idToString.size());
    }
  }

  public static void write(String file_name) throws IOException {
    synchronized (lock) {
      File vocab_file = new File(file_name);
      DataOutputStream vocab_stream =
          new DataOutputStream(new BufferedOutputStream(new FileOutputStream(vocab_file)));
      vocab_stream.writeInt(idToString.size() - 1);
      logger.info("Writing vocabulary: " + (idToString.size() - 1) + " tokens.");
      for (int i = 1; i < idToString.size(); i++) {
        vocab_stream.writeInt(i);
        vocab_stream.writeUTF(idToString.get(i));
      }
      vocab_stream.close();
    }
  }

  public static void freeze() {
    synchronized (lock) {
      int current_id = 1;

      TreeMap<Long, Integer> hash_to_id = new TreeMap<Long, Integer>();
      ArrayList<String> id_to_string = new ArrayList<String>(idToString.size() + 1);
      id_to_string.add(UNKNOWN_ID, UNKNOWN_WORD);

      Map.Entry<Long, Integer> walker = hashToId.firstEntry();
      while (walker != null) {
        String word = hashToString.get(walker.getKey());
        hash_to_id.put(walker.getKey(), (walker.getValue() < 0 ? -current_id : current_id));
        id_to_string.add(current_id, word);
        current_id++;
        walker = hashToId.higherEntry(walker.getKey());
      }
      idToString = id_to_string;
      hashToId = hash_to_id;
    }
  }

  public static int id(String token) {
    synchronized (lock) {
      long hash = 0;
      try {
        hash = MurmurHash.hash64(token);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      String hash_word = hashToString.get(hash);
      if (hash_word != null) {
        if (!token.equals(hash_word)) {
          logger.warning("MurmurHash for the following symbols collides: '" + hash_word + "', '"
              + token + "'");
        }
        return hashToId.get(hash);
      } else {
        int id = idToString.size() * (nt(token) ? -1 : 1);

        // register this (token,id) mapping with each language
        // model, so that they can map it to their own private
        // vocabularies
        for (NGramLanguageModel lm : lms)
          lm.registerWord(token, Math.abs(id));

        idToString.add(token);
        hashToString.put(hash, token);
        hashToId.put(hash, id);
        return id;
      }
    }
  }

  public static boolean hasId(int id) {
    synchronized (lock) {
      id = Math.abs(id);
      return (id < idToString.size());
    }
  }

  public static int[] addAll(String sentence) {
    return addAll(sentence.split("\\s+"));
  }
  
  public static int[] addAll(String[] tokens) {
    int[] ids = new int[tokens.length];
    for (int i = 0; i < tokens.length; i++)
      ids[i] = id(tokens[i]);
    return ids;
  }

  public static String word(int id) {
    id = Math.abs(id);
    return idToString.get(id);
  }

  public static String getWords(int[] ids) {
    if (ids.length == 0) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ids.length - 1; i++)
      sb.append(word(ids[i])).append(" ");
    return sb.append(word(ids[ids.length - 1])).toString();
  }

  public static String getWords(final Iterable<Integer> ids) {
    StringBuilder sb = new StringBuilder();
    for (int id : ids)
      sb.append(word(id)).append(" ");
    return sb.deleteCharAt(sb.length() - 1).toString();
  }
  
  private static boolean isNonterminal(String word) {
    return (word.substring(0,1).equals("[") && (word.substring(word.length() - 1,word.length()).equals("]")));
  }
  
  /**
   * This method returns a list of all indices corresponding to Nonterminals in the Vocabulary
   * @return
   */
  public static List<Integer> getNonterminalIndices()
  {
    List<Integer> result = new ArrayList<Integer>();
    for(int i = 0; i < idToString.size(); i++)
    {
      String word = idToString.get(i);
      if(isNonterminal(word)){  
        result.add(i);
      }
    } 
    return result;
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
    synchronized (lock) {
      return idToString.size();
    }
  }

  public static int getTargetNonterminalIndex(int id) {
    return FormatUtils.getNonterminalIndex(word(id));
  }

  private static void clear() {
    hashToId = new TreeMap<Long, Integer>();
    hashToString = new TreeMap<Long, String>();
    idToString = new ArrayList<String>();

    idToString.add(UNKNOWN_ID, UNKNOWN_WORD);
  }

  /**
   * Used to indicate that a query has been made for a symbol that is not known.
   * 
   * @author Lane Schwartz
   */
  public static class UnknownSymbolException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an exception indicating that the specified identifier cannot be found in the
     * symbol table.
     * 
     * @param id Integer identifier
     */
    public UnknownSymbolException(int id) {
      super("Identifier " + id + " cannot be found in the symbol table");
    }

    /**
     * Constructs an exception indicating that the specified symbol cannot be found in the symbol
     * table.
     * 
     * @param symbol String symbol
     */
    public UnknownSymbolException(String symbol) {
      super("Symbol " + symbol + " cannot be found in the symbol table");
    }
  }

  /**
   * Used to indicate that word hashing has produced a collision.
   * 
   * @author Juri Ganitkevitch
   */
  public static class HashCollisionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HashCollisionException(String first, String second) {
      super("MurmurHash for the following symbols collides: '" + first + "', '" + second + "'");
    }
  }

  public static Iterator<String> wordIterator() {
    return idToString.iterator();
  }
}
