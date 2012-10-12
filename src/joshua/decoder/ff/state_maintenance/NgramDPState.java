package joshua.decoder.ff.state_maintenance;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;

/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class NgramDPState implements DPState {

  private List<Integer> leftLMStateWords;
  private List<Integer> rightLMStateWords;
  private String sig = null;

  private static String SIG_SEP = " -S- "; // seperator for state in signature

  public NgramDPState(List<Integer> leftLMStateWords, List<Integer> rightLMStateWords) {
    this.leftLMStateWords = leftLMStateWords;
    this.rightLMStateWords = rightLMStateWords;
  }

  // construct an instance from the signature string
  public NgramDPState(String sig) {
    this.sig = sig;
    String[] states = sig.split(SIG_SEP); // TODO: use joshua.util.Regex
    this.leftLMStateWords = intArrayToList(Vocabulary.addAll(states[0]));
    this.rightLMStateWords = intArrayToList(Vocabulary.addAll(states[1]));
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

  /*
   * BUG: now, the getSignature is also got called by diskgraph; this may change the this.sig from
   * integers to strings
   */
  public String getSignature(boolean forceRecompute) {
    if (forceRecompute || sig == null) {
      StringBuffer sb = new StringBuffer();
      // sb.append(SIG_PREAMBLE);//TODO: do we really need this

      /**
       * we can not simply use sb.append(leftLMStateWords), as it will just add the address of
       * leftLMStateWords.
       */
      computeStateSig(leftLMStateWords, sb);

      sb.append(SIG_SEP);// TODO: do we really need this

      computeStateSig(rightLMStateWords, sb);

      this.sig = sb.toString();
    }
    // System.out.println("lm sig is:" + this.sig);
    return this.sig;
  }



  private void computeStateSig(List<Integer> state, StringBuffer sb) {

    if (null != state) {
      for (int i = 0; i < state.size(); i++) {
        if (true
        // TODO: equivalnce: number of <null> or <bo>?
        /*
         * states[i]!=Symbol.NULL_RIGHT_LM_STATE_SYM_ID &&
         * states[i]!=Symbol.NULL_LEFT_LM_STATE_SYM_ID && states[i]!=Symbol.LM_STATE_OVERLAP_SYM_ID
         */
        ) {
          sb.append(Vocabulary.word(state.get(i)));
          if (i < state.size() - 1) {
            sb.append(' ');
          }
        }
      }
    } else {
      throw new RuntimeException("state is null");
    }
  }

  private List<Integer> intArrayToList(int[] words) {
    List<Integer> res = new ArrayList<Integer>();
    for (int wrd : words)
      res.add(wrd);
    return res;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<");
    for (int id: leftLMStateWords)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" |");
    for (int id: rightLMStateWords)
      sb.append(" " + Vocabulary.word(id));
    sb.append(" >");
    return sb.toString();
  }
 }
