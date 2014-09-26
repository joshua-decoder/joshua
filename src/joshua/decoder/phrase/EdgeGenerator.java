package joshua.decoder.phrase;

import java.util.List;
import java.util.PriorityQueue;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.segment_file.Sentence;

public class EdgeGenerator {

  private PriorityQueue<Candidate> generate;
  private List<FeatureFunction> featureFunctions;
  private Sentence sentence;
  private JoshuaConfiguration config;
  
  public EdgeGenerator(Sentence sentence, List<FeatureFunction> features, JoshuaConfiguration config) {
    // TODO: does the comparator need to be reversed to put highest-scoring
    // items at the top?
    generate = new PriorityQueue<Candidate>(1);

    this.featureFunctions = features;
    this.sentence = sentence;
    this.config = config;
  }

  /**
   * Receives a partially-initialized translation candidate and places it on the
   * priority queue after scoring it with all of the feature functions. In this
   * respect it is like {@link CubePruneState} (it could make use of that class with
   * a little generalization of spans / coverage).
   * 
   * @param cand
   */
  public void AddCandidate(Candidate cand) {

    // TODO: sourcepath
    ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, cand.getRule(),
        cand.getTailNodes(), -1, cand.getSpan().end, null, this.sentence);
    cand.setResult(result);
    
    generate.add(cand);
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
    System.err.println("EdgeGenerator::Search(): pop: " + to_pop + " empty: " + generate.isEmpty());
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
