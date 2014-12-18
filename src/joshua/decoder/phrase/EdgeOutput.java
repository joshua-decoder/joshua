package joshua.decoder.phrase;

import java.util.HashMap;

import joshua.decoder.Decoder;

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
      stack.add(added);
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

  public void FinishedSearch() {
  }
}
