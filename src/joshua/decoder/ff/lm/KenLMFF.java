package joshua.decoder.ff.lm;

import java.util.LinkedList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.Support;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.FeatureFunction.Accumulator;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.lm.kenlm.jni.KenLM.StateProbPair;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.KenLMState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * Wrapper for KenLM LMs with left-state minimization. We inherit from the regular
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class KenLMFF extends LanguageModelFF {

  public KenLMFF(FeatureVector weights, String featureName, KenLM lm) {
    super(weights, featureName, lm);
  }

  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   * 
   * 
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    int[] ruleWords = rule.getEnglish();

    // The IDs we'll pass to KenLM
    long[] words = new long[ruleWords.length];

    for (int x = 0; x < ruleWords.length; x++) {
      int id = ruleWords[x];

      if (Vocabulary.nt(id)) {
        // Nonterminal: retrieve the KenLM long that records the state
        int index = -(id + 1);
        KenLMState state = (KenLMState) tailNodes.get(index).getDPState(stateIndex);
        words[x] = -state.getState();

      } else {
        // Terminal: just add it
        words[x] = id;
      }
    }

    // Get the probability of applying the rule and the new state
    StateProbPair pair = ((KenLM) languageModel).prob(words);

    // Record the prob
    acc.add(name, pair.prob);

    // Return the state
    return pair.state;
  }

  /**
   * This function differs from regular transitions because we incorporate the cost of incomplete
   * left-hand ngrams, as well as including the start- and end-of-sentence markers (if they were
   * requested when the object was created).
   * 
   * KenLM already includes the prefix probabilities (of shorter n-grams on the left-hand side), so
   * there's nothing that needs to be done.
   */
  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID,
      Accumulator acc) {

//    KenLMState state = (KenLMState) tailNode.getDPState(getStateIndex());

    // This is unnecessary
    //acc.add(name, 0.0f);

    // The state is the same since no rule was applied
    return new KenLMState();
  }

  /**
   * KenLM probs already include the prefix probabilities (they are substracted out when merging
   * states), so this doesn't need to do anything.
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, int sentID) {
    return 0.0f;
  }
}
