package joshua.decoder.ff.state_maintenance;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class NgramStateComputer implements StateComputer<NgramDPState>,
    Comparable<NgramStateComputer> {

  private int ngramOrder;

  private static final Logger logger = Logger.getLogger(NgramStateComputer.class.getName());

  // StateID should be any integer except -1.
  public NgramStateComputer(int nGramOrder) {
    this.ngramOrder = nGramOrder;
    logger.info("NgramStateComputer, order=" + this.ngramOrder);
  }

  public int getOrder() {
    return ngramOrder;
  }

  @Override
  public int compareTo(NgramStateComputer otherState) {
    if (this == otherState)
      return 0;
    else
      return -1;
  }

  public NgramDPState computeFinalState(HGNode tailNode, int i, int j, SourcePath srcPath) {
    // No state is required.
    return null;
  }

  public NgramDPState computeState(Rule rule, List<HGNode> tail_nodes, int span_start,
      int span_end, SourcePath src_path) {
    int[] tgt = rule.getEnglish();

    int[] left = new int[ngramOrder - 1];
    int lcount = 0;

    for (int c = 0; c < tgt.length && lcount < left.length; ++c) {
      int curID = tgt[c];
      if (Vocabulary.idx(curID)) {
        int index = -(curID + 1);
        if (logger.isLoggable(Level.FINEST))
          logger.finest("Looking up state at: " + index);
        NgramDPState tail_state = (NgramDPState) tail_nodes.get(index).getDPState(this);
        int[] leftContext = tail_state.getLeftLMStateWords();
        for (int i = 0; i < leftContext.length && lcount < left.length; i++)
          left[lcount++] = leftContext[i];
      } else {
        left[lcount++] = curID;
      }
    }

    int[] right = new int[ngramOrder - 1];
    int rcount = right.length - 1;

    for (int c = tgt.length - 1; c >= 0 && rcount >= 0; --c) {
      int curID = tgt[c];
      if (Vocabulary.idx(curID)) {
        int index = -(curID + 1);
        if (logger.isLoggable(Level.FINEST))
          logger.finest("Looking up state at: " + index);
        NgramDPState tail_state = (NgramDPState) tail_nodes.get(index).getDPState(this);
        int[] rightContext = tail_state.getRightLMStateWords();
        for (int i = rightContext.length - 1; i >= 0 && rcount >= 0; --i)
          right[rcount--] = rightContext[i];
      } else {
        right[rcount--] = curID;
      }
    }
    return new NgramDPState(Arrays.copyOfRange(left, 0, lcount), Arrays.copyOfRange(right,
        rcount + 1, right.length));
  }
}
