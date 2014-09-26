package joshua.decoder.phrase;

import java.util.List;

import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class Hypothesis extends HGNode implements Comparable<Hypothesis> {

  private Rule rule;
  private Coverage coverage;

  public String toString() {
    return String.format("HYP[%s] %.5f %d", coverage, score, j);
  }

  // Initialize root hypothesis. Provide the LM's BeginSentence.
  public Hypothesis(List<DPState> states, float score) {
    // super(start, end, lhs, dpstates, hyperedge, pruningestimate);
    super(0, 0, -1, states, null, score);
    this.rule = null;
    this.coverage = new Coverage();
  }

  public Hypothesis(Candidate cand) {
    super(cand.span.start, cand.span.end, -1, cand.getStates(), null, 0.0f);
    
    this.coverage = cand.getCoverage();
    
    HyperEdge edge = new HyperEdge(cand.getRule(), 0.0f, 0.0f, cand.getTailNodes(), null);
  }
  
  // Extend a previous hypothesis.
  public Hypothesis(List<DPState> states, float score, Hypothesis previous, int source_begin,
      int source_end, Rule target) {
//  super(source_begin, source_end, -1, null, new HyperEdge(), score);
  super(source_begin, source_end, -1, null, null, score);
    this.rule = target;
    this.coverage = previous.coverage;
    this.coverage.Set(source_begin, source_end);
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

  public Hypothesis Previous() {
    // TODO: return previous hypothesis
    return null;
  }

  public Rule Target() {
    return rule;
  }

  @Override
  public int hashCode() {
    return LastSourceIndex() * GetCoverage().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Hypothesis) {
      Hypothesis other = (Hypothesis) obj;
      if (LastSourceIndex() == other.LastSourceIndex() && GetCoverage() == other.GetCoverage())
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
