package joshua.decoder.phrase;

/*** 
 * A candidate is basically a cube prune state. It contains a list of hypotheses and target
 * phrases, and an instantiated candidate is a pair of indices that index these two lists. This
 * is the "cube prune" position.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.corpus.Span;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class Candidate {

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
    return 17 * hypotheses.size() 
        + 23 * phrases.size() 
        + 57 * span.hashCode() 
        + 117 * Arrays.hashCode(ranks);
//    return hypotheses.hashCode() * phrases.hashCode() * span.hashCode() * Arrays.hashCode(ranks);
  }
  
  @Override
  public String toString() {
    return String.format("CANDIDATE(hyp %d/%d, phr %d/%d) [%s] phrase=[%s] span=%s",
        ranks[0], hypotheses.size(), ranks[1], phrases.size(),
        getHypothesis(), getRule().getEnglishWords().replaceAll("\\[.*?\\] ",""), getSpan());
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
  
  /**
   * Returns the input span from which the phrases for this candidates were gathered.
   * 
   * @return the span object
   */
  public Span getSpan() {
    return this.span;
  }
  
  /**
   * A candidate is a (hypothesis, target phrase) pairing. The hypothesis and target phrase are
   * drawn from a list that is indexed by (ranks[0], ranks[1]), respectively. This is a shortcut
   * to return the hypothesis of the candidate pair.
   * 
   * @return the hypothesis at position ranks[0]
   */
  public Hypothesis getHypothesis() {
    return this.hypotheses.get(ranks[0]);
  }
  
  /**
   * This returns the target side {@link Phrase}, which is a {@link Rule} object. This is just a
   * convenience function that works by returning the phrase indexed in ranks[1].
   * 
   * @return the phrase at position ranks[1]
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
  
  /**
   * Returns the bit vector of this hypothesis. The bit vector is computed by ORing the coverage
   * vector of the tail node (hypothesis) and the source span of phrases in this candidate.
   * @return
   */
  public Coverage getCoverage() {
    Coverage cov = new Coverage(getHypothesis().getCoverage());
    cov.set(getSpan());
    return cov;
  }

  /**
   * Sets the result of a candidate (should just be moved to the constructor).
   * 
   * @param result
   */
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
    return getHypothesis().getScore() + future_delta + result.getTransitionCost();
  }
  
  public float getFutureEstimate() {
    return getHypothesis().getScore() + future_delta;
  }
  
  public List<DPState> getStates() {
    return result.getDPStates();
  }

  public ComputeNodeResult getResult() {
    return result;
  }
}
