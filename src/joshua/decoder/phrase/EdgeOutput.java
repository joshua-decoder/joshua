package joshua.decoder.phrase;

import java.util.HashMap;

// c++: TODO n-best lists.
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
   */
  public void NewHypothesis(Candidate complete) {
    Stacks.AppendToStack(complete, stack);
    
    System.err.println("EdgeOutput::NewHypothesis()");
    
    Hypothesis added = stack.get(stack.size() - 1);
    if (deduper.containsKey(added)) {
      Hypothesis existing = deduper.get(added);
      if (existing.Score() < added.Score())
        existing = added;
      stack.remove(stack.size() - 1);
    }
  }

  public void FinishedSearch() {
  }
}
