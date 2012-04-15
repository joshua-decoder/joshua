package joshua.decoder.ff.state_maintenance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;


public class NgramStateComputer implements StateComputer<NgramDPState> {

  private int ngramOrder;
  private int stateID;

  private static final Logger logger = Logger.getLogger(NgramStateComputer.class.getName());

  // StateID should be any integer except -1.
  public NgramStateComputer(int nGramOrder, int stateID) {
    this.ngramOrder = nGramOrder;
    this.stateID = stateID;
    logger.info("NgramStateComputer: stateID=" + stateID + "; ngramOrder=" + this.ngramOrder);
  }

  public int getStateID() {
    return stateID;
  }


  public void setStateID(int stateID) {
    this.stateID = stateID;
  }


  public NgramDPState computeFinalState(HGNode antNode, int spanStart, int spanEnd,
      SourcePath srcPath) {
    // no state is required
    return null;
  }


  public NgramDPState computeState(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
      SourcePath srcPath) {

    List<Integer> leftStateSequence = new ArrayList<Integer>();
    List<Integer> currentNgram = new ArrayList<Integer>();

    int hypLen = 0;
    int[] enWords = rule.getEnglish();

    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];
      if (Vocabulary.idx(curID)) {
        // == get left- and right-context
        int index = -(curID + 1);

        if (logger.isLoggable(Level.FINEST)) logger.finest("Looking up state at: " + index);

        NgramDPState antState = (NgramDPState) antNodes.get(index).getDPState(this.getStateID());// TODO
        List<Integer> leftContext = antState.getLeftLMStateWords();
        List<Integer> rightContext = antState.getRightLMStateWords();

        if (leftContext.size() != rightContext.size()) {
          throw new RuntimeException(
              "NgramStateComputer.computeState: left and right contexts have unequal lengths");
        }

        // ================ left context
        for (int i = 0; i < leftContext.size(); i++) {
          int t = leftContext.get(i);
          currentNgram.add(t);

          // always calculate cost for <bo>: additional backoff weight
          /*
           * if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) { int numAdditionalBackoffWeight =
           * currentNgram.size() - (i+1);//number of non-state words
           * 
           * //compute additional backoff weight transitionCost -=
           * this.lmGrammar.logProbOfBackoffState(currentNgram, currentNgram.size(),
           * numAdditionalBackoffWeight);
           * 
           * if (currentNgram.size() == this.ngramOrder) { currentNgram.remove(0); } } else
           */if (currentNgram.size() == this.ngramOrder) {
            // compute the current word probablity, and remove it
            // transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);

            currentNgram.remove(0);
          }

          if (leftStateSequence.size() < this.ngramOrder - 1) {
            leftStateSequence.add(t);
          }
        }

        // ================ right context
        // note: left_state_org_wrds will never take words from right context because it is either
        // duplicate or out of range
        // also, we will never score the right context probablity because they are either duplicate
        // or partional ngram
        int tSize = currentNgram.size();
        for (int i = 0; i < rightContext.size(); i++) {
          // replace context
          currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
        }

      } else {// terminal words
        hypLen++;
        currentNgram.add(curID);
        if (currentNgram.size() == this.ngramOrder) {
          // compute the current word probablity, and remove it
          // transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);


          currentNgram.remove(0);
        }
        if (leftStateSequence.size() < this.ngramOrder - 1) {
          leftStateSequence.add(curID);
        }
      }
    }


    // ===== get left euquiv state
    // double[] lmLeftCost = new double[2];
    // int[] equivLeftState =
    // this.lmGrammar.leftEquivalentState(Support.subIntArray(leftLMStateWrds, 0,
    // leftLMStateWrds.size()), this.ngramOrder, lmLeftCost);


    // ===== trabsition and estimate cost
    // transitionCost += lmLeftCost[0];//add finalized cost for the left state words
    // left and right should always have the same size
    List<Integer> rightStateSequence = currentNgram;
    if (leftStateSequence.size() > rightStateSequence.size()) {
      throw new RuntimeException("left has a bigger size right; " + "; left="
          + leftStateSequence.size() + "; right=" + rightStateSequence.size());
    }
    while (rightStateSequence.size() > leftStateSequence.size()) {
      rightStateSequence.remove(0);// TODO: speed up
    }

    return new NgramDPState(leftStateSequence, rightStateSequence);
  }

}
