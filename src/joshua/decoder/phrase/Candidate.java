package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import joshua.corpus.Span;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class Candidate implements Comparator<Candidate>, Comparable<Candidate> {

  // the set of hypotheses that can be paired with phrases from this span 
  private Vertex hypotheses;

  // the list of target phrases gathered from a span of the input
  private TargetPhrases phrases;

  // source span of new phrase
  public Span span;
  
  // indices into the hypotheses and phrases arrays (used for cube pruning)
  private int[] ranks;
  
  // scoring and state information 
  private ComputeNodeResult result;
  
  public Candidate(float futureCost) {
    this.hypotheses = new Vertex();
    this.phrases = new TargetPhrases();
    this.span = new Span(0,1);
    this.ranks = new int[] { 0, 0 };
  }
  
  public Candidate(Vertex hypotheses, TargetPhrases phrases, Span span) {
    this.hypotheses = hypotheses;
    this.phrases = phrases;
    this.span = span;
    this.ranks = new int[] { 0, 0 };
    
    
  }

  public Candidate(Vertex hypotheses, TargetPhrases phrases, Span span, int[] ranks) {
    this.hypotheses = hypotheses;
    this.phrases = phrases;
    this.span = span;
    this.ranks = ranks;
//    this.score = hypotheses.get(ranks[0]).score + phrases.get(ranks[1]).getEstimatedCost();
  }
  
  public Candidate[] extend() {
    Candidate[] extend = new Candidate[] { null, null };
    if (ranks[0] < hypotheses.size() - 1) {
      extend[0] = new Candidate(hypotheses, phrases, span, new int[] { ranks[0] + 1, ranks[1] });
    }
    
    if (ranks[1] < phrases.size() - 1) {
      extend[1] = new Candidate(hypotheses, phrases, span, new int[] { ranks[0], ranks[1] + 1 });
    }
    
    return extend;
  }
  
  @Override
  public int compareTo(Candidate other) {
    return Float.compare(score(), other.score());
  }

  @Override
  public int compare(Candidate arg0, Candidate arg1) {
    return Float.compare(arg0.score(), arg1.score());
  }

  public Span getSpan() {
    return this.span;
  }
  
  public Hypothesis getHypothesis() {
    return this.hypotheses.get(ranks[0]).history;
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
    if (result != null)
      return this.hypotheses.get(ranks[0]).score + result.getTransitionCost();
    return 0.0f;
  }
  
  public List<DPState> getStates() {
    return result.getDPStates();
  }

  public ComputeNodeResult getResult() {
    return result;
  }
}
