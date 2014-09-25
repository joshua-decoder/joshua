package joshua.decoder.phrase;

/***
 * Examines a series of hypotheses, retaining the best one. This best hypothesis is appended
 * to the stack when the search is finished.
 */

public class PickBest implements Output {
  
  private Stack stack;
  private Candidate best;

  public PickBest(Stack stack) {
    this.stack = stack;
    this.stack.clear();
//    this.stack.reserve(1);
    this.best = null;
  }
  
  public void NewHypothesis(Candidate complete) {
    if (best == null || complete.compareTo(best) == 1) {
      best = complete;
    }
  }
  
  public void FinishedSearch() {
    if (best != null) 
      Stacks.AppendToStack(best, stack);
  }
}
