package joshua.decoder.hypergraph;

import static java.lang.Integer.MAX_VALUE;
import static joshua.corpus.Vocabulary.getWords;
import static joshua.corpus.Vocabulary.nt;

import java.util.Stack;

import joshua.decoder.ff.tm.Rule;

public class ViterbiOutputStringWalkerFunction implements WalkerFunction {
  
  private Stack<int[]> viterbiWords = new Stack<int[]>();

  @Override
  public void apply(HGNode node) {
    final Rule rule = node.bestHyperedge.getRule();
    if (rule != null) {
      merge(rule.getEnglish());
    }
  }
  
  private boolean containsNonTerminals(final int[] ids) {
    boolean hasNonTerminals = false;
    for (int i = 0; i < ids.length; i++) {
      if (nt(ids[i])) {
        hasNonTerminals = true;
        break;
      }
    }
    return hasNonTerminals;
  }
  
  /**
   * Returns the index of the next non-terminal slot to fill.
   * Since non-terminals in right hand sides of rules are indexed by
   * their order on the source side, this function looks for the largest
   * negative id in ids and returns its index. 
   */
  private int getNextNonTerminalIndexToFill(final int[] ids) {
    int nextIndex = 0;
    int nextNonTerminal = -MAX_VALUE;
    for (int i = 0; i < ids.length; i++) {
      if (nt(ids[i]) && ids[i] > nextNonTerminal) {
        nextIndex = i;
        nextNonTerminal = ids[i];
      }
    }
    return nextIndex;
  }
  
  private int[] substituteNonTerminal(final int[] parentWords, final int[] childWords) {
    final int ntIndex = getNextNonTerminalIndexToFill(parentWords);
    final int[] result = new int[parentWords.length + childWords.length - 1];
    int resultIndex = 0;
    for (int i = 0; i < ntIndex; i++) {
      result[resultIndex++] = parentWords[i];
    }
    for (int i = 0; i < childWords.length; i++) {
      result[resultIndex++] = childWords[i];
    }
    for (int i = ntIndex + 1; i < parentWords.length; i++) {
      result[resultIndex++] = parentWords[i];
    }
    return result;
  }

  private void merge(final int[] words) {
    if (!containsNonTerminals(words)
        && !viterbiWords.isEmpty()
        && containsNonTerminals(viterbiWords.peek())) {
      merge(substituteNonTerminal(viterbiWords.pop(), words));
    } else {
      viterbiWords.add(words);
    }
  }
  
  @Override
  public String toString() {
    if (viterbiWords.isEmpty()) {
      return "";
    }
    
    if (viterbiWords.size() != 1) {
      throw new RuntimeException(
          String.format(
              "Stack of ViterbiOutputStringWalker should contain only a single (last) element, but was size %d", viterbiWords.size()));
    }
    
    String result = getWords(viterbiWords.peek());
    // strip of sentence markers (<s>,</s>)
    result = result.substring(result.indexOf(' ') + 1, result.lastIndexOf(' '));
    return result.trim();
  }
  
}