package joshua.decoder.phrase;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.segment_file.Sentence;

public class EdgeGenerator {

  private PriorityQueue<Candidate> generate;
  private List<FeatureFunction> featureFunctions;
  private Sentence sentence;
  private JoshuaConfiguration config;
  private HashSet<Candidate> visitedStates;
  
  public EdgeGenerator(Sentence sentence, List<FeatureFunction> features, JoshuaConfiguration config) {
    // TODO: does the comparator need to be reversed to put highest-scoring
    // items at the top?
    generate = new PriorityQueue<Candidate>(1);

    this.featureFunctions = features;
    this.sentence = sentence;
    this.config = config;
    
    this.visitedStates = new HashSet<Candidate>();
  }

  /**
   * Receives a partially-initialized translation candidate and places it on the
   * priority queue after scoring it with all of the feature functions. In this
   * respect it is like {@link CubePruneState} (it could make use of that class with
   * a little generalization of spans / coverage).
   * 
   * This function is also used to (fairly concisely) implement constrained decoding. Before
   * adding a candidate, we ensure that the sequence of English words match the sentence. If not,
   * the code extends the dot in the cube-pruning chart to the next phrase, since that one might
   * be a match.
   * 
   * @param cand
   */
  public void addCandidate(Candidate cand) {
    if (visitedStates.contains(cand))
      return;
    
    visitedStates.add(cand);

    // Constrained decoding
    if (sentence.target() != null) {
      String oldWords = cand.getHypothesis().bestHyperedge.getRule().getEnglishWords().replace("[X,1] ",  "");
      String newWords = cand.getRule().getEnglishWords().replace("[X,1] ",  "");
          
      // If the string is not found in the target sentence, explore the cube neighbors
      if (sentence.fullTarget().indexOf(oldWords + " " + newWords) == -1) {
        Candidate next = cand.extendPhrase();
        if (next != null)
          addCandidate(next); 
        return;
      }
    }

    // TODO: sourcepath
    ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, cand.getRule(),
        cand.getTailNodes(), -1, cand.getSpan().end, null, this.sentence);
    cand.setResult(result);
    
//    System.err.println("addCandidate(): " + cand);

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
    assert !generate.isEmpty();

    // This is what we'll return, but first we have to do some expansion
    Candidate top = generate.poll();

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
    if (Decoder.VERBOSE >= 3) {
      System.err.println("EdgeGenerator::Search(): pop: " + to_pop + " size: " + generate.size());
      for (Candidate c: generate)
        System.err.println("  " + c);
    }
    while (to_pop > 0 && !generate.isEmpty()) {
      Candidate got = Pop();
      if (got != null) {
        output.NewHypothesis(got);
        --to_pop;
        
        for (Candidate c : got.extend())
          if (c != null) {
            addCandidate(c);
          }
      }
    }
  }
}
