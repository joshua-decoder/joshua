package joshua.decoder.ff.lm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.Support;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
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

  public static int START_SYM_ID;
  public static int STOP_SYM_ID;

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
  protected final NGramLanguageModel languageModel;

  /**
   * We always use this order of ngram, though the LMGrammar may provide higher order probability.
   */
  protected final int ngramOrder;

  /**
   * We cache the weight of the feature since there is only one.
   */
  protected float weight;

  // boolean add_boundary=false; //this is needed unless the text already has <s> and </s>

  /**
   * stateID is any integer exept -1
   **/
  public LanguageModelFF(FeatureVector weights, String featureName, NGramLanguageModel lm) {
    super(weights, featureName);
    this.languageModel = lm;
    this.ngramOrder = lm.getOrder();
    this.START_SYM_ID = Vocabulary.id(Vocabulary.START_SYM);
    this.STOP_SYM_ID = Vocabulary.id(Vocabulary.STOP_SYM);

    if (!weights.containsKey(name))
      System.err.println("* WARNING: no weight found for LanguageModelFF '" + name + "'");

    this.weight = weights.get(name);
  }
  
  public NGramLanguageModel getLM() {
    return this.languageModel;
  }

  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    NgramDPState newState = null;
    if (rule != null)
      newState = computeTransition(rule.getEnglish(), tailNodes, acc);

    return newState;
  }

  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID,
      Accumulator acc) {

    NgramDPState newState = computeFinalTransition((NgramDPState)tailNode.getDPState(stateIndex), acc);
    return newState;
  }

  /**
   * This function computes all the complete n-grams found in the rule, as well as the incomplete
   * n-grams on the left-hand side.
   */
  @Override
  public float estimateCost(Rule rule, int sentID) {

    float estimate = 0.0f;
    boolean considerIncompleteNgrams = true;

    int[] enWords = rule.getEnglish();

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

    return weight * estimate;
  }

  /**
   * Estimates the future cost of a rule. For the language model feature, this is the sum of the
   * costs of the leftmost k-grams, k = [1..n-1].
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, int sentID) {
    /* TODO: This does not work when addStart == true or addEnd == true */
    boolean addStart = false;
    boolean addEnd = false;

    NgramDPState state = (NgramDPState) currentState;

    float estimate = 0.0f;
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
      estimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
    }

    if (addEnd == true) {
      int[] rightContext = state.getRightLMStateWords();
      List<Integer> list = new ArrayList<Integer>(rightContext.length);
      for (int w : rightContext)
        list.add(w);
      list.add(STOP_SYM_ID);
      float tem = scoreChunkLogP(list, false, false);
      estimate += tem;
    }

    return weight * estimate;
  }

  /**
   * Compute the cost of a rule application. The cost of applying a rule is computed by determining
   * the n-gram costs for all n-grams created by this rule application, and summing them. N-grams
   * are created when (a) terminal words in the rule string are followed by a nonterminal (b)
   * terminal words in the rule string are preceded by a nonterminal (c) we encounter adjacent
   * nonterminals. In all of these situations, the corresponding boundary words of the node in the
   * hypergraph represented by the nonterminal must be retrieved.
   */
  private NgramDPState computeTransition(int[] enWords, List<HGNode> tailNodes, Accumulator acc) {

    LinkedList<Integer> currentNgram = new LinkedList<Integer>();
    float transitionLogP = 0.0f;

    LinkedList<Integer> left = new LinkedList<Integer>();
    LinkedList<Integer> right = new LinkedList<Integer>();

    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);

        NgramDPState state = (NgramDPState) tailNodes.get(index).getDPState(stateIndex);
        int[] leftContext = state.getLeftLMStateWords();
        int[] rightContext = state.getRightLMStateWords();
        
        // Left context.
        for (int i = 0; i < leftContext.length; i++) {
          int t = leftContext[i];
          currentNgram.add(t);

          // Compute context
          if (left.size() < ngramOrder - 1)
            left.add(t);

          if (currentNgram.size() == this.ngramOrder) {
            // Compute the current word probability, and remove it.s
            float prob = this.languageModel.ngramLogProbability(Support.toArray(currentNgram),
                this.ngramOrder);
            // System.err.println(String.format("NGRAM(%s) = %.5f",
            // Vocabulary.getWords(currentNgram), prob));
            transitionLogP += prob;
            currentNgram.removeFirst();
          }
        }

        // Right context.
        int tSize = currentNgram.size();
        for (int i = 0; i < rightContext.length; i++) {
          // replace context
          currentNgram.set(tSize - rightContext.length + i, rightContext[i]);

          right.add(rightContext[i]);
          if (right.size() > ngramOrder - 1)
            right.remove(0);
        }
      } else { // terminal words
        currentNgram.add(curID);
        if (currentNgram.size() == this.ngramOrder) {
          // compute the current word probablity, and remove it
          float prob = this.languageModel.ngramLogProbability(Support.toArray(currentNgram),
              this.ngramOrder);
          transitionLogP += prob;
          currentNgram.remove(0);
        }

        // Compute context
        if (left.size() < ngramOrder - 1)
          left.add(curID);
        right.add(curID);
        if (right.size() > ngramOrder - 1)
          right.remove(0);
      }
    }

    acc.add(name, transitionLogP);
    return new NgramDPState(Support.toArray(left), Support.toArray(right));
  }

  /**
   * This function differs from regular transitions because we incorporate the cost of incomplete
   * left-hand ngrams, as well as including the start- and end-of-sentence markers (if they were
   * requested when the object was created).
   * 
   * @param state the dynamic programming state
   * @return the final transition probability (including incomplete n-grams)
   */
  private NgramDPState computeFinalTransition(NgramDPState state, Accumulator acc) {

    float res = 0.0f;
    LinkedList<Integer> currentNgram = new LinkedList<Integer>();
    int[] leftContext = state.getLeftLMStateWords();
    int[] rightContext = state.getRightLMStateWords();

    for (int i = 0; i < leftContext.length; i++) {
      int t = leftContext[i];
      currentNgram.add(t);

      if (currentNgram.size() >= 2) { // start from bigram
        float prob = this.languageModel.ngramLogProbability(Support.toArray(currentNgram),
            currentNgram.size());
        res += prob;
      }
      if (currentNgram.size() == this.ngramOrder)
        currentNgram.removeFirst();
    }

    // Tell the accumulator
    acc.add(name, res);

    // State is the same
    return new NgramDPState(leftContext, rightContext);
  }

  /**
   * This function is basically a wrapper for NGramLanguageModel::sentenceLogProbability(). It
   * computes the probability of a phrase ("chunk"), using lower-order n-grams for the first n-1
   * words.
   * 
   * @param words
   * @param considerIncompleteNgrams
   * @param skipStart
   * @return the phrase log probability
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
      return (float) this.languageModel.sentenceLogProbability(
          Support.subIntArray(words, 0, words.size()), this.ngramOrder, startIndex);
    }
  }
}