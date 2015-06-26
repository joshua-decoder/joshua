package joshua.decoder.ff.lm;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.lm.berkeley_lm.LMGrammarBerkeley;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

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

  private static int LM_INDEX = 0;
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
  protected NGramLanguageModel languageModel;

  /**
   * We always use this order of ngram, though the LMGrammar may provide higher order probability.
   */
  protected final int ngramOrder;

  /**
   * We cache the weight of the feature since there is only one.
   */
  protected float weight;
  protected String type;
  protected String path;

  /* Whether this is a class-based LM */
  private boolean isClassLM;
  private ClassMap classMap;
  
  protected class ClassMap {

    private final int OOV_id = 10;
    private HashMap<Integer, Integer> classMap;

    public ClassMap(String file_name) throws IOException {
      this.classMap = new HashMap<Integer, Integer>();
      read(file_name);
    }

    public int getClassID(int wordID) {
      if (this.classMap.containsKey(wordID)) {
        return this.classMap.get(wordID);
      } else {
        return OOV_id;
      }
    }

    /**
     * Reads a class map from file.
     * 
     * @param file_name
     * @throws IOException
     */
    private void read(String file_name) throws IOException {

      int lineno = 0;
      for (String line: new joshua.util.io.LineReader(file_name, false)) {
        lineno++;
        String[] lineComp = line.trim().split("\\s+");
        try {
          this.classMap.put(Vocabulary.id(lineComp[0]), Integer.parseInt(lineComp[1]));
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
          System.err.println(String.format("* WARNING: bad vocab line #%d '%s'", lineno, line));
        }
      }
    }

  }

  public LanguageModelFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, String.format("lm_%d", LanguageModelFF.LM_INDEX++), args, config);

    this.type = parsedArgs.get("lm_type");
    this.ngramOrder = Integer.parseInt(parsedArgs.get("lm_order")); 
    this.path = parsedArgs.get("lm_file");
    if (parsedArgs.containsKey("class_map"))
      try {
        this.isClassLM = true;
        this.classMap = new ClassMap(parsedArgs.get("class_map"));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    this.weight = weights.get(name);
    
    initializeLM();
  }

  /**
   * Initializes the underlying language model.
   * 
   * @param config
   * @param type
   * @param path
   */
  public void initializeLM() {
    if (type.equals("kenlm")) {
      this.languageModel = new KenLM(ngramOrder, path);
    
    } else if (type.equals("berkeleylm")) {
      this.languageModel = new LMGrammarBerkeley(ngramOrder, path);

    } else {
      System.err.println(String.format("* FATAL: Invalid backend lm_type '%s' for LanguageModel", type));
      System.err.println(String.format("*        Permissible values for 'lm_type' are 'kenlm' and 'berkeleylm'"));
      System.exit(-1);
    }

    Vocabulary.registerLanguageModel(this.languageModel);
    Vocabulary.id(config.default_non_terminal);
    
    LanguageModelFF.START_SYM_ID = Vocabulary.id(Vocabulary.START_SYM);
    LanguageModelFF.STOP_SYM_ID = Vocabulary.id(Vocabulary.STOP_SYM);
  }

  public NGramLanguageModel getLM() {
    return this.languageModel;
  }
  
  public String logString() {
    if (languageModel != null)
      return String.format("%s, order %d (weight %.3f)", name, languageModel.getOrder(), weight);
    else
      return "WHOA";
  }

  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    NgramDPState newState = null;
    if (rule != null) {
      if (config.source_annotations) {
        // Get source side annotations and project them to the target side
        newState = computeTransition(getTags(rule, i, j, sentence), tailNodes, acc);
      }
      else {
        if (this.isClassLM) {
          // Use a class language model
          // Return target side classes
          newState = computeTransition(getClasses(rule), tailNodes, acc);
        }
        else {
          // Default LM 
          newState = computeTransition(rule.getEnglish(), tailNodes, acc);
        }
      }
    
    }
    
    return newState;
  }

  /**
   * Input sentences can be tagged with information specific to the language model. This looks for
   * such annotations by following a word's alignments back to the source words, checking for
   * annotations, and replacing the surface word if such annotations are found.
   * 
   */
  protected int[] getTags(Rule rule, int begin, int end, Sentence sentence) {
    /* Very important to make a copy here, so the original rule is not modified */
    int[] tokens = Arrays.copyOf(rule.getEnglish(), rule.getEnglish().length);
    byte[] alignments = rule.getAlignment();

//    System.err.println(String.format("getTags() %s", rule.getRuleString()));
    
    /* For each target-side token, project it to each of its source-language alignments. If any of those
     * are annotated, take the first annotation and quit.
     */
    if (alignments != null) {
      for (int i = 0; i < tokens.length; i++) {
        if (tokens[i] > 0) { // skip nonterminals
          for (int j = 0; j < alignments.length; j += 2) {
            if (alignments[j] == i) {
              int annotation = sentence.getAnnotation((int)alignments[i] + begin);
              if (annotation != -1) {
//                System.err.println(String.format("  word %d source %d abs %d annotation %d/%s", 
//                    i, alignments[i], alignments[i] + begin, annotation, Vocabulary.word(annotation)));
                tokens[i] = annotation;
                break;
              }
            }
          }
        }
      }
    }
    
    return tokens;
  }
  
  /** 
   * Sets the class map if this is a class LM 
   * @param classMap
   * @throws IOException 
   */
  public void setClassMap(String fileName) throws IOException {
    this.classMap = new ClassMap(fileName);
  }
  
  
  /**
   * Replace each word in a rule with the target side classes.
   */
  protected int[] getClasses(Rule rule) {
    if (this.classMap == null) {
      System.err.println("The class map is not set. Cannot use the class LM ");
      System.exit(2);
    }
    /* Very important to make a copy here, so the original rule is not modified */
    int[] tokens = Arrays.copyOf(rule.getEnglish(), rule.getEnglish().length);
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i] > 0 ) {
        tokens[i] = this.classMap.getClassID(tokens[i]);
      }
    }
    return tokens;
  }

  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {
    return computeFinalTransition((NgramDPState) tailNode.getDPState(stateIndex), acc);
  }

  /**
   * This function computes all the complete n-grams found in the rule, as well as the incomplete
   * n-grams on the left-hand side.
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {

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
  public float estimateFutureCost(Rule rule, DPState currentState, Sentence sentence) {
    NgramDPState state = (NgramDPState) currentState;

    float estimate = 0.0f;
    int[] leftContext = state.getLeftLMStateWords();

    if (null != leftContext) {
      List<Integer> words = new ArrayList<Integer>();
      for (int w : leftContext)
        words.add(w);

      boolean considerIncompleteNgrams = true;
      boolean skipStart = true;
      if (words.get(0) != START_SYM_ID) {
        skipStart = false;
      }
      estimate += scoreChunkLogP(words, considerIncompleteNgrams, skipStart);
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
   * 
   * IMPORTANT: only complete n-grams are scored. This means that hypotheses with fewer words
   * than the complete n-gram state remain *unscored*. This fact adds a lot of complication to the
   * code, including the use of the computeFinal* family of functions, which correct this fact for
   * sentences that are too short on the final transition.
   */
  private NgramDPState computeTransition(int[] enWords, List<HGNode> tailNodes, Accumulator acc) {

    int[] current = new int[this.ngramOrder];
    int[] shadow = new int[this.ngramOrder];
    int ccount = 0;
    float transitionLogP = 0.0f;
    int[] left_context = null;
    
    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);

        NgramDPState state = (NgramDPState) tailNodes.get(index).getDPState(stateIndex);
        int[] left = state.getLeftLMStateWords();
        int[] right = state.getRightLMStateWords();

        // Left context.
        for (int i = 0; i < left.length; i++) {
          current[ccount++] = left[i];

          if (left_context == null && ccount == this.ngramOrder - 1)
            left_context = Arrays.copyOf(current, ccount);

          if (ccount == this.ngramOrder) {
            // Compute the current word probability, and remove it.
            float prob = this.languageModel.ngramLogProbability(current, this.ngramOrder);
//            System.err.println(String.format("-> prob(%s) = %f", Vocabulary.getWords(current), prob));
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

        if (left_context == null && ccount == this.ngramOrder - 1)
          left_context = Arrays.copyOf(current, ccount);

        if (ccount == this.ngramOrder) {
          // Compute the current word probability, and remove it.s
          float prob = this.languageModel.ngramLogProbability(current, this.ngramOrder);
//          System.err.println(String.format("-> prob(%s) = %f", Vocabulary.getWords(current), prob));
          transitionLogP += prob;
          System.arraycopy(current, 1, shadow, 0, this.ngramOrder - 1);
          int[] tmp = current;
          current = shadow;
          shadow = tmp;
          --ccount;
        }
      }
    }
    acc.add(name, transitionLogP);

    if (left_context != null) {
      return new NgramDPState(left_context, Arrays.copyOfRange(current, ccount - this.ngramOrder
          + 1, ccount));
    } else {
      int[] context = Arrays.copyOf(current, ccount);
      return new NgramDPState(context, context);
    }
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

//    System.err.println(String.format("LanguageModel::computeFinalTransition()"));
    
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

    float score = 0.0f;
    if (words.size() > 0) {
      int startIndex;
      if (!considerIncompleteNgrams) {
        startIndex = this.ngramOrder;
      } else if (skipStart) {
        startIndex = 2;
      } else {
        startIndex = 1;
      }
      score = this.languageModel.sentenceLogProbability(
          Support.subIntArray(words, 0, words.size()), this.ngramOrder, startIndex);
    }

    return score;
  }
}
