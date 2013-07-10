package joshua.decoder.ff.lm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.Support;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This class performs the following:
 * <ol>
 * <li>Gets the additional LM score due to combinations of small items into larger ones by using
 * rules
 * <li>Gets the LM state
 * <li>Gets the left-side LM state estimation score
 * </ol>
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class LanguageModelFF extends StatefulFF {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(LanguageModelFF.class.getName());

  private final int START_SYM_ID;
  private final int STOP_SYM_ID;

  private final boolean addStartAndEndSymbol = false;

  /**
   * N-gram language model. We assume the language model is in ARPA format for equivalent state:
   * 
   * <ol>
   * <li>We assume it is a backoff lm, and high-order ngram implies low-order ngram; absense of
   * low-order ngram implies high-order ngram</li>
   * <li>For a ngram, existence of backoffweight => existence a probability Two ways of dealing with
   * low counts:
   * <ul>
   * <li>SRILM: don't multiply zeros in for unknown words</li>
   * <li>Pharaoh: cap at a minimum score exp(-10), including unknown words</li>
   * </ul>
   * </li>
   */
  private final NGramLanguageModel lmGrammar;

  /**
   * We always use this order of ngram, though the LMGrammar may provide higher order probability.
   */
  private final int ngramOrder;

  /**
   * We cache the weight of the feature since there is only one.
   */
  private float weight;

  // boolean add_boundary=false; //this is needed unless the text already has <s> and </s>

  /**
   * stateID is any integer exept -1
   **/
  public LanguageModelFF(FeatureVector weights, String featureName, NGramLanguageModel lm,
      StateComputer state) {
    super(weights, featureName, state);
    this.lmGrammar = lm;
    this.ngramOrder = lm.getOrder();
    this.START_SYM_ID = Vocabulary.id(Vocabulary.START_SYM);
    this.STOP_SYM_ID = Vocabulary.id(Vocabulary.STOP_SYM);

    if (!weights.containsKey(name))
      System.err.println("* WARNING: no weight found for LanguageModelFF '" + name + "'");

    this.weight = weights.get(name);
  }

  /**
   * Computes the cost of the transition, which is the inner product of the feature value computed
   * along this edge times the feature weight.
   * 
   * @return the transition cost
   */
  @Override
  public float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath srcPath,
      int sentID) {
    return weight * computeTransition(rule.getEnglish(), tailNodes);
  }

  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   */
  public FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {
    FeatureVector transitionFeatures = null;
    if (rule != null)
      transitionFeatures = new FeatureVector(name, computeTransition(rule.getEnglish(), tailNodes));
    else
      transitionFeatures = new FeatureVector();

    return transitionFeatures;
  }

  /**
   * Returns the feature accumulated over the final, top-level, rule-less transition.
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return
   */
  public FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID) {
    return new FeatureVector(name, computeFinalTransition((NgramDPState) tailNode.getDPState(this
        .getStateComputer())));
  }

  /**
   * The final cost of an edge differs from compute the regular cost because we add in the cost of
   * all incomplete bigrams on the lefthand side.
   */
  public float computeFinalCost(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID) {
    return weight
        * computeFinalTransition((NgramDPState) tailNode.getDPState(this.getStateComputer()));
  }

  /**
   * This function computes all the complete n-grams found in the rule, as well as the incomplete
   * n-grams on the left-hand side. * into its left side.
   */
  @Override
  public float estimateCost(Rule rule, int sentID) {
    return weight * estimateRuleLogProb(rule.getEnglish());
  }

  /**
   * Estimates the future cost of a rule. For the language model feature, this is the sum of the
   * costs of the leftmost k-grams, k = [1..n-1].
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, int sentID) {
    return weight * estimateStateLogProb((NgramDPState) currentState, false, false);
  }

  /**
   * Compute the cost of a rule application. The cost of applying a rule is computed by determining
   * the n-gram costs for all n-grams created by this rule application, and summing them. N-grams
   * are created when (a) terminal words in the rule string are followed by a nonterminal (b)
   * terminal words in the rule string are preceded by a nonterminal (c) we encounter adjacent
   * nonterminals. In all of these situations, the corresponding boundary words of the node in the
   * hypergraph represented by the nonterminal must be retrieved.
   */
  private float computeTransition(int[] enWords, List<HGNode> tailNodes) {

    int[] current = new int[this.ngramOrder];
    int[] shadow = new int[this.ngramOrder];
    int ccount = 0;
    float transitionLogP = 0.0f;

    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);

        NgramDPState state = (NgramDPState) tailNodes.get(index)
            .getDPState(this.getStateComputer());
        int[] left = state.getLeftLMStateWords();
        int[] right = state.getRightLMStateWords();

        // Left context.
        for (int i = 0; i < left.length; i++) {
          current[ccount++] = left[i];
          if (ccount == this.ngramOrder) {
            // Compute the current word probability, and remove it.s
            float prob = this.lmGrammar.ngramLogProbability(current, this.ngramOrder);
            transitionLogP += prob;
            
            System.arraycopy(current, 1, shadow, 0, this.ngramOrder - 1);
            int[] tmp = current;
            current = shadow;
            shadow = tmp;
            --ccount;
          }
        }
        System.arraycopy(right, 0, current, ccount - right.length, right.length);
      } else { // terminal words
        current[ccount++] = curID;
        if (ccount == this.ngramOrder) {
          // Compute the current word probability, and remove it.s
          float prob = this.lmGrammar.ngramLogProbability(current, this.ngramOrder);
          transitionLogP += prob;
          
          System.arraycopy(current, 1, shadow, 0, this.ngramOrder - 1);
          int[] tmp = current;
          current = shadow;
          shadow = tmp;
          --ccount;
        }
      }
    }
    return transitionLogP;
  }

  /**
   * This function differs from regular transitions because we incorporate the cost of incomplete
   * left-hand ngrams, as well as including the start- and end-of-sentence markers (if they were
   * requested when the object was created).
   * 
   * @param state the dynamic programming state
   * @return the final transition probability (including incomplete n-grams)
   */
  private float computeFinalTransition(NgramDPState state) {

    float res = 0.0f;
    LinkedList<Integer> currentNgram = new LinkedList<Integer>();
    int[] leftContext = state.getLeftLMStateWords();
    int[] rightContext = state.getRightLMStateWords();

    // ================ left context
    if (addStartAndEndSymbol)
      currentNgram.add(START_SYM_ID);

    for (int i = 0; i < leftContext.length; i++) {
      int t = leftContext[i];
      currentNgram.add(t);

      if (currentNgram.size() >= 2) { // start from bigram
        float prob = this.lmGrammar.ngramLogProbability(this.toArray(currentNgram),
            currentNgram.size());
        // System.err.println(String.format("NGRAM(%s) = %.5f", Vocabulary.getWords(currentNgram),
        // prob));
        res += prob;
      }
      if (currentNgram.size() == this.ngramOrder)
        currentNgram.removeFirst();
    }

    // ================ right context
    // switch context, we will never score the right context probability because they are either
    // duplicate or partial ngrams
    if (addStartAndEndSymbol) {
      int tSize = currentNgram.size();
      for (int i = 0; i < rightContext.length; i++)
        currentNgram.removeLast();
      for (int i = 0; i < rightContext.length; i++)
        currentNgram.add(rightContext[i]);

      currentNgram.add(STOP_SYM_ID);
      float prob = this.lmGrammar.ngramLogProbability(this.toArray(currentNgram),
          currentNgram.size());
      res += prob;
      // System.err.println(String.format("NGRAM(%s) = %.5f", Vocabulary.getWords(currentNgram),
      // prob));
    }
    return res;
  }

  /*
   * This function computes a language model estimate of a rule. This can be done by computing the
   * probability of all incomplete n-grams found in the rule, for n > 2.
   */
  private float estimateRuleLogProb(int[] enWords) {
    float estimate = 0.0f;
    boolean considerIncompleteNgrams = true;
    List<Integer> words = new ArrayList<Integer>();
    boolean skipStart = (enWords[0] == START_SYM_ID);

    /*
     * Move through the words, accumulating language model costs each time we have an n-gram (n >=
     * 2), and resetting the series of words when we hit a nonterminal.
     */
    for (int c = 0; c < enWords.length; c++) {
      int currentWord = enWords[c];
      if (Vocabulary.nt(currentWord)) {
        estimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
        words.clear();
        skipStart = false;
      } else {
        words.add(currentWord);
      }
    }
    estimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
    return estimate;
  }

  /**
   * TODO: This does not work when addStart == true or addEnd == true
   **/
  private float estimateStateLogProb(NgramDPState state, boolean addStart, boolean addEnd) {

    float res = 0.0f;
    int[] leftContext = state.getLeftLMStateWords();

    if (null != leftContext) {
      List<Integer> words = new ArrayList<Integer>();
      if (addStart == true)
        words.add(START_SYM_ID);
      for (int w : leftContext)
        words.add(w);

      boolean considerIncompleteNgrams = true;
      boolean skipStart = true;
      if (words.get(0) != START_SYM_ID) {
        skipStart = false;
      }
      res += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
    }

    if (addEnd == true) {
      int[] rightContext = state.getRightLMStateWords();
      List<Integer> list = new ArrayList<Integer>(rightContext.length);
      for (int w : rightContext)
        list.add(w);
      list.add(STOP_SYM_ID);
      float tem = scoreChunkLogP(list, false, false);
      res += tem;
    }
    return res;
  }

  /**
   * @param words
   * @param considerIncompleteNgrams
   * @param skipStart
   * @return
   */
  private float scoreChunkLogP(List<Integer> words, boolean considerIncompleteNgrams,
      boolean skipStart) {
    if (words.size() <= 0) {
      return 0.0f;
    } else {
      int startIndex;
      if (!considerIncompleteNgrams) {
        startIndex = this.ngramOrder;
      } else if (skipStart) {
        startIndex = 2;
      } else {
        startIndex = 1;
      }
      // System.err.println("Estimate: " + Vocabulary.getWords(words));
      return (float) this.lmGrammar.sentenceLogProbability(
          Support.subIntArray(words, 0, words.size()), this.ngramOrder, startIndex);
    }
  }

  private final int[] toArray(List<Integer> input) {
    int[] output = new int[input.size()];
    int i = 0;
    for (int v : input)
      output[i++] = v;
    return output;
  }
}
