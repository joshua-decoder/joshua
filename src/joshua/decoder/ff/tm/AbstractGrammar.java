package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.segment_file.Token;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;

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
  
  /*
   * The maximum length of a source-side phrase. Mostly used by the phrase-based decoder.
   */
  protected int maxSourcePhraseLength = -1;
  
    /**
   * Returns the longest source phrase read.
   * 
   * @return the longest source phrase read (nonterminal + terminal symbols).
   */
  @Override
  public int getMaxSourcePhraseLength() {
    return maxSourcePhraseLength;
  }
  
  @Override
  public int getOwner() {
    return owner;
  }

  /* The maximum span of the input this rule can be applied to. */
  protected int spanLimit = 1;

  protected JoshuaConfiguration joshuaConfiguration;

  /**
   * Constructs an empty, unsorted grammar.
   * 
   * @see Grammar#isSorted()
   */
  public AbstractGrammar(JoshuaConfiguration config) {
    this.joshuaConfiguration = config;
    this.sorted = false;
  }

  public AbstractGrammar(int owner, int spanLimit) {
    this.sorted = false;
    this.owner = owner;
    this.spanLimit = spanLimit;
  }

  public static final int OOV_RULE_ID = 0;

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

        /* This causes the rules at this trie node to be sorted */
        rules.getSortedRules(models);

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
  
  /**
   * Adds OOV rules for all words in the input lattice to the current grammar. Uses addOOVRule() so that
   * sub-grammars can define different types of OOV rules if needed (as is used in {@link PhraseTable}).
   * 
   * @param inputLattice the lattice representing the input sentence
   * @param featureFunctions a list of feature functions used for scoring
   */
  public static void addOOVRules(Grammar grammar, Lattice<Token> inputLattice, 
      List<FeatureFunction> featureFunctions, boolean onlyTrue) {
    /*
     * Add OOV rules; This should be called after the manual constraints have
     * been set up.
     */
    HashSet<Integer> words = new HashSet<Integer>();
    for (Node<Token> node : inputLattice) {
      for (Arc<Token> arc : node.getOutgoingArcs()) {
        // create a rule, but do not add into the grammar trie
        // TODO: which grammar should we use to create an OOV rule?
        int sourceWord = arc.getLabel().getWord();
        if (sourceWord == Vocabulary.id(Vocabulary.START_SYM)
            || sourceWord == Vocabulary.id(Vocabulary.STOP_SYM))
          continue;

        // Determine if word is actual OOV.
        if (onlyTrue && ! Vocabulary.hasId(sourceWord))
          continue;

        words.add(sourceWord);
      }
    }

    for (int sourceWord: words) 
      grammar.addOOVRules(sourceWord, featureFunctions);

    // Sort all the rules (not much to actually do, this just marks it as sorted)
    grammar.sortGrammar(featureFunctions);
  }
}
