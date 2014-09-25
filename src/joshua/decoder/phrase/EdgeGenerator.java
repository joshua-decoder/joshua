package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.segment_file.Sentence;

public class EdgeGenerator {

  private PriorityQueue<Candidate> generate;
  private List<FeatureFunction> featureFunctions;
  private int sentID;
  private JoshuaConfiguration config;
  
  public EdgeGenerator(Sentence sentence, List<FeatureFunction> features, JoshuaConfiguration config) {
    // TODO: does the comparator need to be reversed to put highest-scoring
    // items at the top?
    generate = new PriorityQueue<Candidate>(1);

    this.featureFunctions = features;
    this.sentID = sentence.id();
    this.config = config;
  }

  /**
   * Receives a partially-initialized translation candidate and places it on the
   * priority queue after scoring it with all of the feature functions. In this
   * respect it is like ComputeNodeResult (it could make use of that class with
   * a little generalization of spans / coverage).
   * 
   * @param cand
   */
  public void AddCandidate(Candidate cand) {
    // TODO: score the candidate here, before adding
    // This seems to be the most general way to do it, barring, of course,
    // whether to score
    // before or after placing on the candidates list

    // TODO: create (and score with LM) new hypothesis
    cand.states = new ArrayList<DPState>();
    float transitionCost = 0.0f, futureCostEstimate = 0.0f;
    for (FeatureFunction feature : featureFunctions) {
      FeatureFunction.ScoreAccumulator acc = feature.new ScoreAccumulator();

      // TODO: sourcePath not implemented
      DPState newState = feature.compute(cand.getRule(), cand.getTailNodes(), -1,
          cand.getTailNodes().get(0).j, null, sentID, acc);
      transitionCost += acc.getScore();

      if (feature.isStateful()) {
        futureCostEstimate += feature.estimateFutureCost(cand.getRule(), newState, sentID);
        cand.states.add(((StatefulFF) feature).getStateIndex(), newState);
      }
    }
    
    cand.score += transitionCost;
    
    generate.add(cand);
  }

  public boolean Empty() {
    return generate.isEmpty();
  }

  /**
   * Repeatedly pop the top hypothesis off the priority queue, record it as a
   * new hypothesis for the current stack, and add its extensions to the
   * priority queue.
   * 
   * @param context
   * @return
   */
  // Pop. If there's a complete hypothesis, return it. Otherwise return an
  // invalid PartialEdge.
  public Candidate Pop() {
    System.err.println("EdgeGenerator::Pop()");
    assert !generate.isEmpty();

    // This is what we'll return, but first we have to do some expansion
    Candidate top = generate.poll();

    for (Candidate c : top.extend())
      if (c != null)
        AddCandidate(c);

    return top;
  }

  /**
   * Cube pruning. Repeatedly pop the top hypothesis, pushing its extensions
   * onto the priority queue.
   * 
   * @param context
   * @param output
   */
  public void Search(Output output) {
    int to_pop = config.pop_limit;
    System.err.println("Search(" + to_pop + " " + generate.isEmpty() + ")");
    while (to_pop > 0 && !generate.isEmpty()) {
      Candidate got = Pop();
      if (got != null) {
        output.NewHypothesis(got);
        --to_pop;
      }
    }
    output.FinishedSearch();
  }
}
