package joshua.decoder.phrase;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class Hypothesis extends HGNode implements Comparable<Hypothesis> {

  // The rule represented by this hypothesis
  private Rule rule;
  
  // The hypothesis' coverage vector
  private Coverage coverage;
  
  public static BilingualRule BEGIN_RULE = new HieroFormatReader().parseLine("[X] ||| <s> ||| <s> ||| 0");
  public static BilingualRule END_RULE = new HieroFormatReader().parseLine("[X] ||| [X,1] </s> ||| [X,1] </s> ||| 0");
      
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (DPState state: getDPStates())
      sb.append("STATE: " + state + " ");
    return String.format("HYP[%s] %.5f %d", coverage, score, j);
  }

  // Initialize root hypothesis. Provide the LM's BeginSentence.
  public Hypothesis(List<DPState> states, float futureCost) {
    super(0, 1, Vocabulary.id("[X]"), states,
        new HyperEdge(BEGIN_RULE, 0.0f, 0.0f, null, null), futureCost);
    
    this.rule = BEGIN_RULE;
    this.coverage = new Coverage(1);
  }

  public Hypothesis(Candidate cand) {
    // TODO: sourcepath
    super(cand.span.start, cand.span.end, Vocabulary.id("[X]"), cand.getStates(), new HyperEdge(
        cand.getRule(), cand.getResult().getViterbiCost(), cand.getResult().getTransitionCost(),
        cand.getTailNodes(), null), cand.score());
    this.coverage = cand.getCoverage();
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
