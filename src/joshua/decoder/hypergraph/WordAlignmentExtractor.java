package joshua.decoder.hypergraph;

import java.util.Stack;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.KBestExtractor.DerivationState;
import joshua.decoder.hypergraph.KBestExtractor.DerivationVisitor;

/**
 * this class implements Joshua's Derivation Visitor interface.
 * before() and after() methods are called at each visit of a rule in 
 * the hypergraph.
 * We place WordAlignmentStates on a stack and merge/substitute them into each
 * other if possible. At the end, the remaining last state on the stack 
 * should be complete (no NonTerminals to substitute anymore).
 */
public class WordAlignmentExtractor implements DerivationVisitor {

  private Stack<WordAlignmentState> stack;

  public WordAlignmentExtractor() {
    stack = new Stack<WordAlignmentState>();
  }

  void merge(WordAlignmentState astate) {
    // if alignment state has no NTs left AND stack is not empty
    // AND parent object on stack still needs something to substitute
    if (astate.isComplete() && stack.size() > 0 && !stack.peek().isComplete()) {
      WordAlignmentState parentState = stack.pop();
      parentState.substituteIn(astate);
      merge(parentState);
    } else {
      stack.add(astate);
    }
  }

  @Override
  public void before(DerivationState state, int level) {
    Rule rule = state.edge.getRule();
    if (rule != null) {
      merge(new WordAlignmentState(rule, state.parentNode.i));
    }
  }

  @Override
  public void after(DerivationState state, int level) {
  }

  public String toString() {
    WordAlignmentState finalState = stack.pop();
    return finalState.toFinalString();
  }
}
