package joshua.decoder.ff.lm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.lm.KenLM;
import joshua.decoder.ff.lm.KenLM.StateProbPair;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.KenLMState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/**
 * Wrapper for KenLM LMs with left-state minimization. We inherit from the regular
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class StateMinimizingLanguageModel extends LanguageModelFF {

  // maps from sentence numbers to KenLM-side pools used to allocate state
  private static final ConcurrentHashMap<Integer, Long> poolMap = new ConcurrentHashMap<Integer, Long>();

  public StateMinimizingLanguageModel(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, args, config);
    this.type = "kenlm";
    if (parsedArgs.containsKey("lm_type") && ! parsedArgs.get("lm_type").equals("kenlm")) {
      System.err.println("* FATAL: StateMinimizingLanguageModel only supports 'kenlm' lm_type backend");
      System.err.println("*        Remove lm_type from line or set to 'kenlm'");
      System.exit(-1);
    }
  }
  
  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    
    ArrayList<String> names = new ArrayList<String>();
    names.add(name);
    return names;
  }

  /**
   * Initializes the underlying language model.
   * 
   * @param config
   * @param type
   * @param path
   */
  @Override
  public void initializeLM() {
    
    // Override type (only KenLM supports left-state minimization)
    this.languageModel = new KenLM(ngramOrder, path);

    Vocabulary.registerLanguageModel(this.languageModel);
    Vocabulary.id(config.default_non_terminal);
    
    LanguageModelFF.START_SYM_ID = Vocabulary.id(Vocabulary.START_SYM);
    LanguageModelFF.STOP_SYM_ID = Vocabulary.id(Vocabulary.STOP_SYM);
  }
  
  /**
   * Estimates the cost of a rule. We override here since KenLM can do it more efficiently
   * than the default {@link LanguageModelFF} class.
   *    
   * Most of this function implementation is redundant with compute().
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    
    int[] ruleWords = rule.getEnglish();

    // The IDs we'll pass to KenLM
    long[] words = new long[ruleWords.length];

    for (int x = 0; x < ruleWords.length; x++) {
      int id = ruleWords[x];

      if (Vocabulary.nt(id)) {
        // For the estimate, we can just mark negative values
        words[x] = -1;

      } else {
        // Terminal: just add it
        words[x] = id;
      }
    }
    
    // Get the probability of applying the rule and the new state
    return weight * ((KenLM) languageModel).estimateRule(words);
  }
  
  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    int[] ruleWords = config.source_annotations 
        ? getTags(rule, i, j, sentence)
        : rule.getEnglish();

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
    
    int sentID = sentence.id();
    if (!poolMap.containsKey(sentID))
      poolMap.put(sentID, KenLM.createPool());

    // Get the probability of applying the rule and the new state
    StateProbPair pair = ((KenLM) languageModel).probRule(words, poolMap.get(sentID));

    // Record the prob
//    acc.add(name, pair.prob);
    acc.add(denseFeatureIndex, pair.prob);

    // Return the state
    return pair.state;
  }

  /**
   * Destroys the pool created to allocate state for this sentence. Called from the
   * {@link joshua.decoder.Translation} class after outputting the sentence or k-best list. Hosting
   * this map here in KenLMFF statically allows pools to be shared across KenLM instances.
   * 
   * @param sentId
   */
  public void destroyPool(int sentId) {
    if (poolMap.containsKey(sentId))
      KenLM.destroyPool(poolMap.get(sentId));
    poolMap.remove(sentId);
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
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {

    // KenLMState state = (KenLMState) tailNode.getDPState(getStateIndex());

    // This is unnecessary
    // acc.add(name, 0.0f);

    // The state is the same since no rule was applied
    return new KenLMState();
  }

  /**
   * KenLM probs already include the prefix probabilities (they are substracted out when merging
   * states), so this doesn't need to do anything.
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, Sentence sentence) {
    return 0.0f;
  }
}
