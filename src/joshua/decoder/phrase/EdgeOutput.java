package joshua.decoder.phrase;

import java.util.HashMap;

/**
 * Responsible for receiving new hypotheses that are generated and adding them to the stack. 
 *
 */
public class EdgeOutput implements Output {
  
  private Stack stack;
  private HashMap<Hypothesis, Hypothesis> deduper;

  public EdgeOutput(Stack stack) {
    this.stack = stack;
    
    this.deduper = new HashMap<Hypothesis,Hypothesis>();
  }
  
  /***
   * Append to the stack. Then check whether the stack already contained an equivalent hypothesis,
   * and remove it if so, keeping the better edge.
   * 
   * TODO: combine backpointers
   */
  public void NewHypothesis(Candidate complete) {
    stack.add(new Hypothesis(complete));
        
    Hypothesis added = stack.get(stack.size() - 1);
    
    System.err.println("EdgeOutput::NewHypothesis() -> " + added);

    if (deduper.containsKey(added)) {
      Hypothesis existing = deduper.get(added);
      if (existing.Score() < added.Score()) {
        added.addHyperedgesInNode(existing.hyperedges);
        existing = added;
      } else {
        existing.addHyperedgesInNode(added.hyperedges);
      }
      
      System.err.println(String.format("-> Edge existed, now has %d incoming arcs", existing.hyperedges.size()));
      
      stack.remove(stack.size() - 1);
    } else {
      deduper.put(added, added);
      System.err.println(String.format("-> Edge was new"));
    }
  }

  public void FinishedSearch() {
  }
}
