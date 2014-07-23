/*
 * This file is based on the edu.umd.clip.mt.Phrase class from the University of Maryland's
 * umd-hadoop-mt-0.01 project. That project is released under the terms of the Apache License 2.0,
 * but with special permission for the Joshua Machine Translation System to release modifications
 * under the LGPL version 2.1. LGPL version 3 requires no special permission since it is compatible
 * with Apache License 2.0
 */
package joshua.corpus;

import java.util.ArrayList;

/**
 * The simplest concrete implementation of Phrase.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class BasicPhrase extends AbstractPhrase {
  private byte language;
  private int[] words;


  public BasicPhrase(byte language, String sentence) {
    this.language = language;
    this.words = splitSentence(sentence);
  }

  private BasicPhrase() {}

  public int[] getWordIDs() {
    return words;
  }

  /* See Javadoc for Phrase interface. */
  public BasicPhrase subPhrase(int start, int end) {
    BasicPhrase that = new BasicPhrase();
    that.language = this.language;
    that.words = new int[end - start + 1];
    System.arraycopy(this.words, start, that.words, 0, end - start + 1);
    return that;
  }

  /* See Javadoc for Phrase interface. */
  public ArrayList<Phrase> getSubPhrases() {
    return this.getSubPhrases(this.size());
  }

  /* See Javadoc for Phrase interface. */
  public ArrayList<Phrase> getSubPhrases(int maxLength) {
    ArrayList<Phrase> phrases = new ArrayList<Phrase>();
    int len = this.size();
    for (int n = 1; n <= maxLength; n++)
      for (int i = 0; i <= len - n; i++)
        phrases.add(this.subPhrase(i, i + n - 1));
    return phrases;
  }

  /* See Javadoc for Phrase interface. */
  public int size() {
    return (words == null ? 0 : words.length);
  }

  /* See Javadoc for Phrase interface. */
  public int getWordID(int position) {
    return words[position];
  }

  /**
   * Returns a human-readable String representation of the phrase.
   * <p>
   * The implementation of this method is slightly more efficient than that inherited from
   * <code>AbstractPhrase</code>.
   * 
   * @return a human-readable String representation of the phrase.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (words != null) {
      for (int i = 0; i < words.length; ++i) {
        if (i != 0) sb.append(' ');
        sb.append(Vocabulary.word(words[i]));
      }
    }
    return sb.toString();
  }
}
