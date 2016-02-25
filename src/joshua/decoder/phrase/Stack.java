package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.segment_file.Sentence;

/**
 * Organizes all hypotheses containing the same number of source words. 
 *
 */
public class Stack extends ArrayList<Hypothesis> {
  
  private static final long serialVersionUID = 7885252799032416068L;

  private HashMap<Coverage, ArrayList<Hypothesis>> coverages;
  
  private Sentence sentence;
  private List<FeatureFunction> featureFunctions;
  private JoshuaConfiguration config;

  /* The list of states we've already visited. */
  private HashSet<Candidate> visitedStates;
  
  /* A list of candidates sorted for consideration for entry to the chart (for cube pruning) */
  private PriorityQueue<Candidate> candidates;
  
  /* Short-circuits adding a cube-prune state more than once */
  private HashMap<Hypothesis, Hypothesis> deduper;
  
  /**
   * Create a new stack. Stacks are organized one for each number of source words that are covered.
   * 
   * @param featureFunctions
   * @param sentence
   * @param config
   */
  public Stack(List<FeatureFunction> featureFunctions, Sentence sentence, JoshuaConfiguration config) {
    this.featureFunctions = featureFunctions;
    this.sentence = sentence;
    this.config = config;
    
    this.candidates = new PriorityQueue<Candidate>(1, new CandidateComparator());
    this.coverages = new HashMap<Coverage, ArrayList<Hypothesis>>();
    this.visitedStates = new HashSet<Candidate>();
    this.deduper = new HashMap<Hypothesis,Hypothesis>();
  }

  /**
   * A Stack is an ArrayList; here, we intercept the add so we can maintain a list of the items
   * stored under each distinct coverage vector
   */
  @Override
  public boolean add(Hypothesis hyp) {
    
    if (! coverages.containsKey((hyp.getCoverage())))
      coverages.put(hyp.getCoverage(), new ArrayList<Hypothesis>()); 
    coverages.get(hyp.getCoverage()).add(hyp);
    
    return super.add(hyp);
  }
  
  /**
   * Intercept calls to remove() so that we can reduce the coverage vector
   */
  @Override
  public boolean remove(Object obj) {
    boolean found = super.remove(obj);
    if (found) {
      Hypothesis item = (Hypothesis) obj;
      Coverage cov = item.getCoverage();
      assert coverages.get(cov).remove(obj);
      if (coverages.get(cov).size() == 0)
        coverages.remove(cov);
    }
    return found;
  }
  
  /** 
   * Returns the set of coverages contained in this stack. This is used to iterate over them
   * in the main decoding loop in Stacks.java.
   */
  public Set<Coverage> getCoverages() {
    return coverages.keySet();
  }
  
  /**
   * Get all items with the same coverage vector.
   * 
   * @param cov
   * @return
   */
  public ArrayList<Hypothesis> get(Coverage cov) {
    ArrayList<Hypothesis> list = coverages.get(cov);
    Collections.sort(list);
    return list;
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
    
    candidates.add(cand);
  }
  
  /**
   * Cube pruning. Repeatedly pop the top candidate, creating a new hyperedge from it, adding it to
   * the k-best list, and then extending the list of candidates with extensions of the current
   * candidate.
   * 
   * @param context
   * @param output
   */
  public void search() {
    int to_pop = config.pop_limit;
    
    if (Decoder.VERBOSE >= 3) {
      System.err.println("Stack::search(): pop: " + to_pop + " size: " + candidates.size());
      for (Candidate c: candidates)
        System.err.println("  " + c);
    }
    while (to_pop > 0 && !candidates.isEmpty()) {
      Candidate got = candidates.poll();
      if (got != null) {
        addHypothesis(got);
        --to_pop;
        
        for (Candidate c : got.extend())
          if (c != null) {
            addCandidate(c);
          }
      }
    }
  }

  /**
   * Adds a popped candidate to the chart / main stack. This is a candidate we have decided to
   * keep around.
   * 
   */
  public void addHypothesis(Candidate complete) {
    Hypothesis added = new Hypothesis(complete);
    
    if (deduper.containsKey(added)) {
      Hypothesis existing = deduper.get(added);
      existing.absorb(added);
      
      if (Decoder.VERBOSE >= 3) {
        System.err.println(String.format("recombining hypothesis from ( ... %s )", complete.getHypothesis().getRule().getEnglishWords()));
        System.err.println(String.format("        base score %.3f", complete.getResult().getBaseCost()));
        System.err.println(String.format("        covering %d-%d", complete.getSpan().start - 1, complete.getSpan().end - 2));
        System.err.println(String.format("        translated as: %s", complete.getRule().getEnglishWords()));
        System.err.println(String.format("        score %.3f + future cost %.3f = %.3f", 
            complete.getResult().getTransitionCost(), complete.getFutureEstimate(),
            complete.getResult().getTransitionCost() + complete.getFutureEstimate()));
      }
      
    } else {
      add(added);
      deduper.put(added, added);
      
      if (Decoder.VERBOSE >= 3) {
        System.err.println(String.format("creating new hypothesis from ( ... %s )", complete.getHypothesis().getRule().getEnglishWords()));
        System.err.println(String.format("        base score %.3f", complete.getResult().getBaseCost()));
        System.err.println(String.format("        covering %d-%d", complete.getSpan().start - 1, complete.getSpan().end - 2));
        System.err.println(String.format("        translated as: %s", complete.getRule().getEnglishWords()));
        System.err.println(String.format("        score %.3f + future cost %.3f = %.3f", 
            complete.getResult().getTransitionCost(), complete.getFutureEstimate(),
            complete.getResult().getTransitionCost() + complete.getFutureEstimate()));
      }
    }
  }
}
