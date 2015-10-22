package joshua.corpus;

import static joshua.util.FormatUtils.isNonterminal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.decoder.Decoder;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.util.FormatUtils;

/**
 * Static singular vocabulary class.
 * Supports (de-)serialization into a vocabulary file.
 * 
 * @author Juri Ganitkevitch
 */

public class Vocabulary {

  private final static ArrayList<NGramLanguageModel> lms = new ArrayList<NGramLanguageModel>();

  private static List<String> idToString;
  private static Map<String, Integer> stringToId;
  
  private static volatile List<Integer> nonTerminalIndices;

  private static final Integer lock = new Integer(0);

  static final int UNKNOWN_ID = 0;
  static final String UNKNOWN_WORD = "<unk>";

  public static final String START_SYM = "<s>";
  public static final String STOP_SYM = "</s>";
  
  static {
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
  public static boolean read(final File vocab_file) throws IOException {
    synchronized (lock) {
      DataInputStream vocab_stream =
          new DataInputStream(new BufferedInputStream(new FileInputStream(vocab_file)));
      int size = vocab_stream.readInt();
      Decoder.LOG(1, String.format("Read %d entries from the vocabulary", size));
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
      Decoder.LOG(1, String.format("Writing vocabulary: %d tokens", idToString.size() - 1));
      for (int i = 1; i < idToString.size(); i++) {
        vocab_stream.writeInt(i);
        vocab_stream.writeUTF(idToString.get(i));
      }
      vocab_stream.close();
    }
  }

  /**
   * Get the id of the token if it already exists, new id is created otherwise.
   * 
   * TODO: currently locks for every call.
   * Separate constant (frozen) ids from changing (e.g. OOV) ids.
   * Constant ids could be immutable -> no locking.
   * Alternatively: could we use ConcurrentHashMap to not have to lock if actually contains it and only lock for modifications? 
   */
  public static int id(String token) {
    synchronized (lock) {
      if (stringToId.containsKey(token)) {
        return stringToId.get(token);
      } else {
        if (nonTerminalIndices != null && nt(token)) {
          throw new IllegalArgumentException("After the nonterminal indices have been set by calling getNonterminalIndices you can't call id on new nonterminals anymore.");
        }
        int id = idToString.size() * (nt(token) ? -1 : 1);

        // register this (token,id) mapping with each language
        // model, so that they can map it to their own private
        // vocabularies
        for (NGramLanguageModel lm : lms)
          lm.registerWord(token, Math.abs(id));

        idToString.add(token);
        stringToId.put(token, id);
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
    synchronized (lock) {
      id = Math.abs(id);
      return idToString.get(id);
    }
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
  
  /**
   * This method returns a list of all (positive) indices
   * corresponding to Nonterminals in the Vocabulary.
   */
  public static List<Integer> getNonterminalIndices()
  {
    if (nonTerminalIndices == null) {
      synchronized (lock) {
        if (nonTerminalIndices == null) {
          nonTerminalIndices = findNonTerminalIndices();
        }
      }
    }
    return nonTerminalIndices;
  }

  /**
   * Iterates over the Vocabulary and finds all non terminal indices.
   */
  private static List<Integer> findNonTerminalIndices() {
    List<Integer> nonTerminalIndices = new ArrayList<Integer>();
    for(int i = 0; i < idToString.size(); i++) {
      final String word = idToString.get(i);
      if(isNonterminal(word)){
        nonTerminalIndices.add(i);
      }
    }
    return nonTerminalIndices;
  }

  public static int getUnknownId() {
    return UNKNOWN_ID;
  }

  public static String getUnknownWord() {
    return UNKNOWN_WORD;
  }

  /**
   * Returns true if the Vocabulary ID represents a nonterminal. 
   * 
   * @param id
   * @return
   */
  public static boolean nt(int id) {
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

  /**
   * Clears the vocabulary and initializes it with an unknown word.
   * Registered language models are left unchanged.
   */
  public static void clear() {
    synchronized (lock) {
      nonTerminalIndices = null;

      idToString = new ArrayList<String>();
      stringToId = new HashMap<String, Integer>();      
  
      idToString.add(UNKNOWN_ID, UNKNOWN_WORD);
      stringToId.put(UNKNOWN_WORD, UNKNOWN_ID);
    }
  }
  
}
