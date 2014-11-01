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
    
//    System.err.println("EdgeOutput::NewHypothesis() -> " + added);
    /*
    System.err.println(String.format("creating new hypothesis from ( ... %s )", complete.getHypothesis().getRule().getEnglishWords()));
    System.err.println(String.format("    covering %d..%d", complete.getSpan().start, complete.getSpan().end));
    System.err.println(String.format("    translated as: %s", complete.getRule().getEnglishWords()));
    System.err.println(String.format("    base score = %.5f", complete.getResult().getBaseCost()));
    System.err.println(String.format("    transition cost = %.5f", complete.getResult().getTransitionCost()));
    System.err.println(String.format("    future cost = %.3f", complete.getFutureEstimate()));
    */
    
    if (deduper.containsKey(added)) {
      Hypothesis existing = deduper.get(added);
      if (existing.Score() < added.Score()) {
        added.addHyperedgesInNode(existing.hyperedges);
        existing = added;
      } else {
        existing.addHyperedgesInNode(added.hyperedges);
      }
      
//      System.err.println(String.format("-> Edge existed, now has %d incoming arcs", existing.hyperedges.size()));
      
      stack.remove(stack.size() - 1);
    } else {
      deduper.put(added, added);
//      System.err.println(String.format("-> Edge was new"));
    }
  }

  public void FinishedSearch() {
  }
}
