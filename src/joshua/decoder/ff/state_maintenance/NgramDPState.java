package joshua.decoder.ff.state_maintenance;

import java.util.Arrays;

import joshua.corpus.Vocabulary;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Juri Ganitkevitch, <juri@cs.jhu.edu>
 */
public class NgramDPState extends DPState {

  private int[] left;
  private int[] right;

  private int hash = 0;

  public NgramDPState(int[] l, int[] r) {
    left = l;
    right = r;
    assertLengths();
  }

  public void setLeftLMStateWords(int[] words) {
    left = words;
    assertLengths();
  }

  public int[] getLeftLMStateWords() {
    return left;
  }

  public void setRightLMStateWords(int[] words) {
    right = words;
    assertLengths();
  }

  public int[] getRightLMStateWords() {
    return right;
  }

  private final void assertLengths() {
    if (left.length != right.length)
      throw new RuntimeException("Unequal lengths in left and right state: < "
          + Vocabulary.getWords(left) + " | " + Vocabulary.getWords(right) + " >");
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = 31 + Arrays.hashCode(left);
      hash = hash * 19 + Arrays.hashCode(right);
    }
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof NgramDPState) {
      NgramDPState that = (NgramDPState) other;
      if (this.left.length == that.left.length && this.right.length == that.right.length) {
        for (int i = 0; i < left.length; ++i)
          if (this.left[i] != that.left[i] || this.right[i] != that.right[i])
            return false;
        return true;
      }
    }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<");
    for (int id : left)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" |");
    for (int id : right)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" >");
    return sb.toString();
  }
}
