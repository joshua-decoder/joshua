package joshua.decoder.ff.tm.hash_based;

import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;

/**
 * Stores a collection of all rules with the same french side (and thus same arity).
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedRuleBin extends BasicRuleCollection {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(MemoryBasedRuleBin.class.getName());

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
      if (logger.isLoggable(Level.SEVERE))
        logger.finest(String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity,
            rule.getArity()));
      return;
    }
    rules.add(rule);
    sorted = false;
    rule.setFrench(this.sourceTokens); // TODO: this will release the memory in each rule, but each
                                       // rule still have a pointer to it
  }
}
