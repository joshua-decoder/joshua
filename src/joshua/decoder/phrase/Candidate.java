package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.corpus.Span;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class Candidate implements Comparable<Candidate> {

  // the set of hypotheses that can be paired with phrases from this span 
  private List<Hypothesis> hypotheses;

  // the list of target phrases gathered from a span of the input
  private TargetPhrases phrases;

  // source span of new phrase
  public Span span;
  
  // future cost of applying phrases to hypotheses
  float future_delta;
  
  // indices into the hypotheses and phrases arrays (used for cube pruning)
  private int[] ranks;
  
  // scoring and state information 
  private ComputeNodeResult result;
  
  /**
   * When candidate objects are extended, the new one is initialized with the same underlying
   * "phrases" and "hypotheses" and "span" objects. So these all have to be equal, as well as
   * the ranks.
   * 
   * This is used to prevent cube pruning from adding the same candidate twice, having reached
   * a point in the cube via different paths.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Candidate) {
      Candidate other = (Candidate) obj;
      if (hypotheses != other.hypotheses || phrases != other.phrases || span != other.span)
        return false;
      
      if (ranks.length != other.ranks.length)
        return false;
      
      for (int i = 0; i < ranks.length; i++)
        if (ranks[i] != other.ranks[i])
          return false;
          
      return true;
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return hypotheses.hashCode() * phrases.hashCode() * span.hashCode() * Arrays.hashCode(ranks);
  }
  
  @Override
  public String toString() {
    return String.format("CAND[%d/%d hypotheses, %d/%d phrases] score=%.3f (base %.3f + future %.3f + trans %.3f) %s + %s (over %s)", ranks[0],
        hypotheses.size(), ranks[1], phrases.size(), score(),
        getHypothesis().Score(),
        getFutureEstimate(),
        result.getTransitionCost() , 
        getHypothesis(), getRule().getEnglishWords(), getSpan());
  }
  
  public Candidate(List<Hypothesis> hypotheses, TargetPhrases phrases, Span span, float delta) {
    this.hypotheses = hypotheses;
    this.phrases = phrases;
    this.span = span;
    this.future_delta = delta;
    this.ranks = new int[] { 0, 0 };
  }

  public Candidate(List<Hypothesis> hypotheses, TargetPhrases phrases, Span span, float delta, int[] ranks) {
    this.hypotheses = hypotheses;
    this.phrases = phrases;
    this.span = span;
    this.future_delta = delta;
    this.ranks = ranks;
//    this.score = hypotheses.get(ranks[0]).score + phrases.get(ranks[1]).getEstimatedCost();
  }
  
  /**
   * Extends the cube pruning dot in both directions and returns the resulting set. Either of the
   * results can be null if the end of their respective lists is reached.
   * 
   * @return The neighboring candidates (possibly null)
   */
  public Candidate[] extend() {
    return new Candidate[] { extendHypothesis(), extendPhrase() };
  }
  
  /**
   * Extends the cube pruning dot along the dimension of existing hypotheses.
   * 
   * @return the next candidate, or null if none
   */
  public Candidate extendHypothesis() {
    if (ranks[0] < hypotheses.size() - 1) {
      return new Candidate(hypotheses, phrases, span, future_delta, new int[] { ranks[0] + 1, ranks[1] });
    }
    return null;
  }
  
  /**
   * Extends the cube pruning dot along the dimension of candidate target sides.
   * 
   * @return the next Candidate, or null if none
   */
  public Candidate extendPhrase() {
    if (ranks[1] < phrases.size() - 1) {
      return new Candidate(hypotheses, phrases, span, future_delta, new int[] { ranks[0], ranks[1] + 1 });
    }
    
    return null;
  }
  
  @Override
  public int compareTo(Candidate other) {
    return Float.compare(other.score(), score());
  }

  public Span getSpan() {
    return this.span;
  }
  
  public Hypothesis getHypothesis() {
    return this.hypotheses.get(ranks[0]);
  }
  
  /**
   * It is sometimes useful to think of a phrase pair like a syntax-based rule. This function returns
   * a Rule view of the candidate by returning the Phrase (which extends Rule) marked by the currently
   * selected rank.
   * 
   * @return
   */
  public Rule getRule() {
    return phrases.get(ranks[1]);
  }
  
  /**
   * The hypotheses list is a list of tail pointers. This function returns the tail pointer
   * currently selected by the value in ranks.
   * 
   * @return a list of size one, wrapping the tail node pointer
   */
  public List<HGNode> getTailNodes() {
    List<HGNode> tailNodes = new ArrayList<HGNode>();
    tailNodes.add(getHypothesis());
    return tailNodes;
  }
  
  public Coverage getCoverage() {
    return getHypothesis().GetCoverage().or(getSpan());
  }

  public void setResult(ComputeNodeResult result) {
    this.result = result;
  }

  /**
   * This returns the sum of two costs: the HypoState cost + the transition cost. The HypoState cost
   * is in turn the sum of two costs: the Viterbi cost of the underlying hypothesis, and the adjustment
   * to the future score incurred by translating the words under the source phrase being added.
   * The transition cost is the sum of new features incurred along the transition (mostly, the
   * language model costs).
   * 
   * The Future Cost item should probably just be implemented as another kind of feature function,
   * but it would require some reworking of that interface, which isn't worth it. 
   * 
   * @return
   */
  public float score() {
    return getHypothesis().Score() + future_delta + result.getTransitionCost();
  }
  
  public float getFutureEstimate() {
    return getHypothesis().Score() + future_delta;
  }
  
  public List<DPState> getStates() {
    return result.getDPStates();
  }

  public ComputeNodeResult getResult() {
    return result;
  }
}
