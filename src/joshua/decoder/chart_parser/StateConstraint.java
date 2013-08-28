package joshua.decoder.chart_parser;

import java.util.Collection;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;

/**
 * This class provides constraints on the sorts of states that are permitted in the chart. Its
 * original motivation was to be used as a means of doing forced decoding, which is accomplished by
 * forcing all n-gram states that are created to match the target string.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * 
 */
public class StateConstraint {
  private String target = null;

  public StateConstraint(String target) {
    this.target = " <s> " + target + " </s> ";
  }

  /**
   * Determines if all of the states passed in are legal in light of the input that was passed
   * earlier. Currently only defined for n-gram states.
   * 
   * @param dpStates
   * @return whether the states are legal in light of the target side sentence
   */
  public boolean isLegal(Collection<DPState> dpStates) {
    /*
     * Iterate over all the state-contributing objects associated with the new state, querying
     * n-gram ones (of which there is probably only one), allowing them to veto the move.
     */
    for (DPState dpState : dpStates) {
      if (dpState instanceof NgramDPState) {
        // Build a regular expression out of the state context.
        String leftWords = " "
            + Vocabulary.getWords(((NgramDPState) dpState).getLeftLMStateWords()) + " ";
        String rightWords = " "
            + Vocabulary.getWords(((NgramDPState) dpState).getRightLMStateWords()) + " ";

        int leftPos = this.target.indexOf(leftWords);
        int rightPos = this.target.lastIndexOf(rightWords);

        boolean legal = (leftPos != -1 && leftPos <= rightPos);
//        System.err.println(String.format("  isLegal(%s @ %d,%s @ %d) = %s", leftWords, leftPos,
//         rightWords, rightPos, legal));

        return legal;
      }
    }

    return true;
  }
}
