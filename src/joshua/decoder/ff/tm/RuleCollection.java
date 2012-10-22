package joshua.decoder.ff.tm;

import java.util.List;

import joshua.decoder.ff.FeatureFunction;

/**
 * Represents a set of rules under a particular TrieGrammar node. Therefore, all the rules under a
 * RuleCollection will share:
 * 
 * <ul>
 * <li>arity</li>
 * <li>source side</li>
 * </ul>
 * 
 * @author Zhifei Li
 * @author Lane Schwartz
 * @author Matt Post <post@cs.jhu.edu>
 */
public interface RuleCollection {

  /**
   * Sorts the grammar rules in this collection using the provided feature functions.
   * 
   * @param weights the model weights.
   */
  void sortRules(List<FeatureFunction> models);

  /**
   * TODO: now, we assume this function will be called only after all the rules have been read; this
   * method need to be synchronized as we will call this function only after the decoding begins to
   * avoid the synchronized method, we should call this once the grammar is finished
   * <p>
   * public synchronized ArrayList<Rule> get_sorted_rules(){ l_models: if it is non-null, then the
   * rules will be sorted using the new feature functions (or new weight), otherwise, just return a
   * sorted list based on the last time of feature functions
   * <p>
   * Only CubePruning requires that rules are sorted based on est_cost (confirmed by zhifei)
   */
  List<Rule> getSortedRules();


  /**
   * get the list of rules (which may not be sorted or not)
   * */
  List<Rule> getRules();


  /**
   * Gets the source side for all rules in this RuleCollection. This source side is the same for all
   * the rules in the RuleCollection.
   * 
   * @return the (common) source side for all rules in this RuleCollection
   */
  int[] getSourceSide();

  /**
   * Gets the number of nonterminals in the source side of the rules in this RuleCollection. The
   * source side is the same for all the rules in the RuleCollection, so the arity will also be the
   * same for all of these rules.
   * 
   * @return the (common) number of nonterminals in the source side of the rules in this
   *         RuleCollection
   */
  int getArity();

}
