package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import joshua.decoder.ff.FeatureFunction;

/**
 * Basic collection of translation rules.
 * 
 * @author Lane Schwartz
 * @author Zhifei Li
 */
public class BasicRuleCollection implements RuleCollection {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(BasicRuleCollection.class.getName());

  /**
   * Indicates whether the rules in this collection have been sorted based on the latest feature
   * function values.
   */
  protected boolean sorted;

  /** List of rules stored in this collection. */
  protected final List<Rule> rules;

  /** Number of nonterminals in the source pattern. */
  protected int arity;

  /**
   * Sequence of terminals and nonterminals in the source pattern.
   */
  protected int[] sourceTokens;

  /**
   * Constructs an initially empty rule collection.
   * 
   * @param arity Number of nonterminals in the source pattern
   * @param sourceTokens Sequence of terminals and nonterminals in the source pattern
   */
  public BasicRuleCollection(int arity, int[] sourceTokens) {
    this.rules = new ArrayList<Rule>();
    this.sourceTokens = sourceTokens;
    this.arity = arity;
    this.sorted = false;
  }

  /**
   * Constructs a rule collection with the given data.
   * <p>
   * The list of rules must already be sorted
   * <p>
   * NOTE: if rules==null, the rule member variable will be initialized to an <em>immutable</em>
   * empty list.
   * 
   * @param arity
   * @param sourceTokens
   * @param rules
   */
  public BasicRuleCollection(int arity, int[] sourceTokens, List<Rule> rules) {
    if (rules == null) {
      this.rules = Collections.<Rule>emptyList();
    } else {
      this.rules = rules;
    }
    this.sourceTokens = sourceTokens;
    this.arity = arity;
    this.sorted = true;
  }

  /* See Javadoc comments for RuleCollection interface. */
  public int getArity() {
    return this.arity;
  }

  public List<Rule> getRules() {
    return this.rules;
  }
  
  public boolean isSorted() {
    synchronized (this) {
      return sorted;
    }
  }

  private void sortRules(List<Rule> rules, List<FeatureFunction> models) {
    // use a priority queue to help sort
    PriorityQueue<Rule> t_heapRules = new PriorityQueue<Rule>(1, Rule.NegativeCostComparator);
    for (Rule rule : rules) {
      rule.estimateRuleCost(models);
      t_heapRules.add(rule);
    }

    // rearrange the sortedRules based on t_heapRules
    rules.clear();
    while (t_heapRules.size() > 0) {
      Rule t_r = t_heapRules.poll();
      rules.add(0, t_r);
    }
  }

  /* See Javadoc comments for RuleCollection interface. */
  public void sortRules(List<FeatureFunction> models) {
    synchronized (this) {
      sortRules(this.rules, models);
      this.sorted = true;
    }
  }

  /* See Javadoc comments for RuleCollection interface. */
  public synchronized List<Rule> getSortedRules(List<FeatureFunction> models) {
    if (! isSorted())
      sortRules(models);

    return this.rules;
  }

  /* See Javadoc comments for RuleCollection interface. */
  public int[] getSourceSide() {
    return this.sourceTokens;
  }
}
