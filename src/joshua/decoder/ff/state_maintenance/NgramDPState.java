package joshua.decoder.ff.state_maintenance;

import java.util.List;

import joshua.corpus.Vocabulary;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Juri Ganitkevitch, <juri@cs.jhu.edu>
 */
public class NgramDPState implements DPState {

  private List<Integer> leftLMStateWords;
  private List<Integer> rightLMStateWords;

  private int hash = 0;

  public NgramDPState(List<Integer> leftLMStateWords, List<Integer> rightLMStateWords) {
    this.leftLMStateWords = leftLMStateWords;
    this.rightLMStateWords = rightLMStateWords;
  }

  public void setLeftLMStateWords(List<Integer> words_) {
    this.leftLMStateWords = words_;
  }

  public List<Integer> getLeftLMStateWords() {
    return this.leftLMStateWords;
  }

  public void setRightLMStateWords(List<Integer> words_) {
    this.rightLMStateWords = words_;
  }

  public List<Integer> getRightLMStateWords() {
    return this.rightLMStateWords;
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = 31 + stateHash(leftLMStateWords);
      hash = hash * 19 + stateHash(rightLMStateWords);
    }
    return hash;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof NgramDPState) {
      NgramDPState that = (NgramDPState) other;
      if (this.leftLMStateWords.size() != that.leftLMStateWords.size()) return false;
      if (this.rightLMStateWords.size() != that.rightLMStateWords.size()) return false;
      for (int i = 0; i < this.leftLMStateWords.size(); ++i)
        if (!this.leftLMStateWords.get(i).equals(that.leftLMStateWords.get(i)))
          return false;
      for (int i = 0; i < this.rightLMStateWords.size(); ++i)
        if (!this.rightLMStateWords.get(i).equals(that.rightLMStateWords.get(i)))
          return false;
      return true;
    }
    return false;
  }

  private int stateHash(List<Integer> state) {
    int state_hash = 17;
    if (null != state)
      for (int i : state)
        state_hash = state_hash * 19 + i;
    return state_hash;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<");
    for (int id : leftLMStateWords)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" |");
    for (int id : rightLMStateWords)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" >");
    return sb.toString();
  }
}
