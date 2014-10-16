package joshua.decoder.phrase;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * Represents a hypothesis, a translation of some coverage of the input. Extends {@link HGNode}, 
 * through a bit of a hack. Whereas (i,j) represents the span of an {@link HGNode}, i here is not used,
 * and j is overloaded to denote the span of the phrase being applied. The complete coverage vector 
 * can be obtained by looking at the tail pointer and casting it.
 * 
 * @author Kenneth Heafield
 * @author Matt Post <post@cs.jhu.edu>
 */
public class Hypothesis extends HGNode implements Comparable<Hypothesis> {

  // The hypothesis' coverage vector
  private Coverage coverage;
  
  public static BilingualRule BEGIN_RULE = new HieroFormatReader().parseLine("[X] ||| <s> ||| <s> ||| 0");
  public static BilingualRule END_RULE = new HieroFormatReader().parseLine("[GOAL] ||| [X,1] </s> ||| [X,1] </s> ||| 0");
      
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (DPState state: getDPStates())
      sb.append("STATE: " + state + " ");
    return String.format("HYP[%s] %.5f j=%d %s", coverage, score, j, sb);
  }

  // Initialize root hypothesis. Provide the LM's BeginSentence.
  public Hypothesis(List<DPState> states, float futureCost) {
    super(0, 1, Vocabulary.id("[X]"), states,
        new HyperEdge(BEGIN_RULE, 0.0f, 0.0f, null, null), futureCost);
    
    this.coverage = new Coverage(1);
  }

  public Hypothesis(Candidate cand) {
    // TODO: sourcepath
    super(-1, cand.span.end, Vocabulary.id("[X]"), cand.getStates(), new HyperEdge(
        cand.getRule(), cand.getResult().getViterbiCost(), cand.getResult().getTransitionCost(),
        cand.getTailNodes(), null), cand.score());
    this.coverage = cand.getCoverage();
  }
  
  // Extend a previous hypothesis.
  public Hypothesis(List<DPState> states, float score, Hypothesis previous, int source_end, Rule target) {
    super(-1, source_end, -1, null, null, score);
    this.coverage = previous.coverage;
    this.coverage.Set(source_end - 1, source_end);
  }

  public Coverage GetCoverage() {
    return coverage;
  }

  public float Score() {
    return score;
  }

  /**
   * HGNodes (designed for chart parsing) maintain a span (i,j). We overload j
   * here to record the index of the last translated source word.
   * 
   * @return
   */
  public int LastSourceIndex() {
    return j;
  }

  @Override
  public int hashCode() {
    return LastSourceIndex() * GetCoverage().hashCode();
  }

  /**
   * Defines equivalence in terms of recombinability. Two hypotheses are recombinable if 
   * all their DP states are the same, their coverage is the same, and they have the next soure
   * index the same.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Hypothesis) {
      Hypothesis other = (Hypothesis) obj;
      for (int i = 0; i < dpStates.size(); i++) {
        if (!dpStates.get(i).equals(other.dpStates.get(i)))
          return false;
      }
      if (LastSourceIndex() == other.LastSourceIndex() && GetCoverage().equals(other.GetCoverage()))
        return true;
    }
    return false;
  }

  @Override
  public int compareTo(Hypothesis o) {
    // TODO: is this the order we want?
    return Float.compare(o.Score(), Score());
  }
}
