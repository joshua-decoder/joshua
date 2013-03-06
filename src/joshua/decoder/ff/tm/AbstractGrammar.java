package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.FeatureFunction;

/**
 * Partial implementation of the <code>Grammar</code> interface that provides logic for sorting a
 * grammar.
 * <p>
 * <em>Note</em>: New classes implementing the <code>Grammar</code> interface should probably
 * inherit from this class, unless a specific sorting technique different from that implemented by
 * this class is required.
 * 
 * @author Zhifei Li
 * @author Lane Schwartz
 * @author Matt Post <post@cs.jhu.edu
 */
public abstract class AbstractGrammar implements Grammar {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(AbstractGrammar.class.getName());

  /**
   * Indicates whether the rules in this grammar have been sorted based on the latest feature
   * function values.
   */
  protected boolean sorted = false;

  /*
   * The grammar's owner, used to determine which weights are applicable to the dense features found
   * within.
   */
  protected int owner = -1;

  /* The maximum span of the input this rule can be applied to. */
  protected int spanLimit = 1;

  /**
   * Constructs an empty, unsorted grammar.
   * 
   * @see Grammar#isSorted()
   */
  public AbstractGrammar() {
    this.sorted = false;
  }

  public AbstractGrammar(int owner, int spanLimit) {
    this.sorted = false;
    this.owner = owner;
    this.spanLimit = spanLimit;
  }

  public static final int OOV_RULE_ID = 0;

  public int getOOVRuleID() {
    return OOV_RULE_ID;
  }

  /**
   * Cube-pruning requires that the grammar be sorted based on the latest feature functions. To
   * avoid synchronization, this method should be called before multiple threads are initialized for
   * parallel decoding
   */
  public void sortGrammar(List<FeatureFunction> models) {
    Trie root = getTrieRoot();
    if (root != null) {
      sort(root, models);
      setSorted(true);
    }
  }

  /* See Javadoc comments for Grammar interface. */
  public boolean isSorted() {
    return sorted;
  }

  /**
   * Sets the flag indicating whether this grammar is sorted.
   * <p>
   * This method is called by {@link #sortGrammar(ArrayList)} to indicate that the grammar has been
   * sorted.
   * 
   * Its scope is protected so that child classes that override <code>sortGrammar</code> will also
   * be able to call this method to indicate that the grammar has been sorted.
   * 
   * @param sorted
   */
  protected void setSorted(boolean sorted) {
    this.sorted = sorted;
    logger.fine("This grammar is now sorted: " + this);
  }

  /**
   * Recursively sorts the grammar using the provided feature functions.
   * <p>
   * This method first sorts the rules stored at the provided node, then recursively calls itself on
   * the child nodes of the provided node.
   * 
   * @param node Grammar node in the <code>Trie</code> whose rules should be sorted.
   * @param models Feature function models to use during sorting.
   */
  private void sort(Trie node, List<FeatureFunction> models) {

    if (node != null) {
      if (node.hasRules()) {
        RuleCollection rules = node.getRuleCollection();
        if (logger.isLoggable(Level.FINE))
          logger.fine("Sorting node " + Arrays.toString(rules.getSourceSide()));

        if (logger.isLoggable(Level.FINEST)) {
          StringBuilder s = new StringBuilder();
          for (Rule r : rules.getSortedRules(models)) {
            s.append("\n\t" + r.getLHS() + " ||| " + Arrays.toString(r.getFrench()) + " ||| "
                + Arrays.toString(r.getEnglish()) + " ||| " + r.getFeatureVector() + " ||| "
                + r.getEstimatedCost() + "  " + r.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(r)));
          }
          logger.finest(s.toString());
        }
      }

      if (node.hasExtensions()) {
        for (Trie child : node.getExtensions()) {
          sort(child, models);
        }
      } else if (logger.isLoggable(Level.FINE)) {
        logger.fine("Node has 0 children to extend: " + node);
      }
    }
  }

  // write grammar to disk
  public void writeGrammarOnDisk(String file) {
  }

}
