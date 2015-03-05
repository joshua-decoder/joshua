package joshua.decoder.ff.tm.hash_based;

import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;

/**
 * Stores a collection of all rules with the same french side (and thus same arity).
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedRuleBin extends BasicRuleCollection {

  /**
   * Constructs an initially empty rule collection.
   * 
   * @param arity Number of nonterminals in the source pattern
   * @param sourceTokens Sequence of terminals and nonterminals in the source pattern
   */
  public MemoryBasedRuleBin(int arity, int[] sourceTokens) {
    super(arity, sourceTokens);
  }

  /**
   * Adds a rule to this collection.
   * 
   * @param rule Rule to add to this collection.
   */
  public void addRule(Rule rule) {
    // XXX This if clause seems bogus.
    if (rules.size() <= 0) { // first time
      this.arity = rule.getArity();
      this.sourceTokens = rule.getFrench();
    }
    if (rule.getArity() != this.arity) {
      return;
    }
    rules.add(rule);
    sorted = false;
    rule.setFrench(this.sourceTokens);
  }
}
