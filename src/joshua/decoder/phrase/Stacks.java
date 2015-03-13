package joshua.decoder.phrase;

/***
 * Entry point for phrase-based decoding, analogous to {@link Chart} for the CKY algorithm. This
 * class organizes all the stacks used for decoding, and is responsible for building them. Stack
 * construction is stack-centric: that is, we loop over the number of source words in increasing sizes;
 * at each step of this iteration, we break the search between smaller stack sizes and source-side
 * phrase sizes.
 * 
 * The end result of decoding is a {@link Hypergraph} with the same format as hierarchical decoding.
 * Phrases are treating as left-branching rules, and the span information (i,j) is overloaded so
 * that i means nothing and j represents the index of the last-translated source word in each
 * hypothesis. This means that most hypergraph code can work without modification. The algorithm 
 * ensures that the coverage vector is consistent but the resulting hypergraph may not be projective,
 * which is different from the CKY algorithm, which does produce projective derivations. 
 * 
 * Lattice decoding is not yet supported (March 2015).
 */

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Span;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;

public class Stacks {

  // The list of stacks, grouped according to number of source words covered
  private List<Stack> stacks;

  // The end state
  private Hypothesis end;
  
  List<FeatureFunction> featureFunctions;

  private Sentence sentence;

  private JoshuaConfiguration config;

  /* Contains all the phrase tables */
  private PhraseChart chart;
  
  /**
   * Entry point. Initialize everything. Create pass-through (OOV) phrase table and glue phrase
   * table (with start-of-sentence and end-of-sentence rules).
   * 
   * @param sentence
   * @param featureFunctions
   * @param grammars
   * @param config
   */
  public Stacks(Sentence sentence, List<FeatureFunction> featureFunctions, Grammar[] grammars, 
      JoshuaConfiguration config) {

    this.sentence = sentence;
    this.featureFunctions = featureFunctions;
    this.config = config;
    
    int num_phrase_tables = 0;
    for (int i = 0; i < grammars.length; i++)
      if (grammars[i] instanceof PhraseTable)
        ++num_phrase_tables;
    
    PhraseTable[] phraseTables = new PhraseTable[num_phrase_tables + 2];
    for (int i = 0, j = 0; i < grammars.length; i++)
      if (grammars[i] instanceof PhraseTable)
        phraseTables[j++] = (PhraseTable) grammars[i];
    
    phraseTables[phraseTables.length - 2] = new PhraseTable("null", config);
    phraseTables[phraseTables.length - 2].addRule(Hypothesis.END_RULE);
    
    phraseTables[phraseTables.length - 1] = new PhraseTable("oov", config);
    AbstractGrammar.addOOVRules(phraseTables[phraseTables.length - 1], sentence.getLattice(), featureFunctions, config.true_oovs_only);
    
    this.chart = new PhraseChart(phraseTables, featureFunctions, sentence, config.num_translation_options);
  }
  
  
  /**
   * The main algorithm. Returns a hypergraph representing the search space.
   * 
   * @return
   */
  public HyperGraph search() {
    
    long startTime = System.currentTimeMillis();
    
    Future future = new Future(chart);
    stacks = new ArrayList<Stack>();
    
    // <s> counts as the first word. Pushing null lets us count from one.
    stacks.add(null);

    // Initialize root hypothesis with <s> context and future cost for everything.
    ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, Hypothesis.BEGIN_RULE,
        null, -1, 1, null, this.sentence);
    Stack firstStack = new Stack(featureFunctions, sentence, config);
    firstStack.add(new Hypothesis(result.getDPStates(), future.Full()));
    stacks.add(firstStack);
    
    // Decode with increasing numbers of source words. 
    for (int source_words = 2; source_words <= sentence.length(); ++source_words) {
      Stack targetStack = new Stack(featureFunctions, sentence, config);
      stacks.add(targetStack);

      // Iterate over stacks to continue from.
      for (int phrase_length = 1; phrase_length <= Math.min(source_words - 1, chart.MaxSourcePhraseLength());
          phrase_length++) {
        int from_stack = source_words - phrase_length;
        Stack tailStack = stacks.get(from_stack);
        
        if (Decoder.VERBOSE >= 3)
          System.err.println(String.format("\n  WORDS %d MAX %d (STACK %d phrase_length %d)", source_words,
              chart.MaxSourcePhraseLength(), from_stack, phrase_length));
        
        // Iterate over antecedents in this stack.
        for (Coverage coverage: tailStack.getCoverages()) {
          ArrayList<Hypothesis> hypotheses = tailStack.get(coverage); 
          
          // the index of the starting point of the first possible phrase
          int begin = coverage.firstZero();
          
          // the absolute position of the ending spot of the last possible phrase
          int last_end = Math.min(coverage.firstZero() + config.reordering_limit, chart.SentenceLength());
          int last_begin = (last_end > phrase_length) ? (last_end - phrase_length) : 0;

          for (begin = coverage.firstZero(); begin <= last_begin; begin++) {
            if (!coverage.compatible(begin, begin + phrase_length) ||
                ! permissible(coverage, begin, begin + phrase_length)) {
              continue;
            }

            Span span = new Span(begin, begin + phrase_length);

            // Don't append </s> until the end
            if (begin == sentence.length() - 1 && source_words != sentence.length()) 
              continue;            

            TargetPhrases phrases = chart.getRange(begin, begin + phrase_length);
            if (phrases == null)
              continue;

            if (Decoder.VERBOSE >= 3)
              System.err.println(String.format("  Applying %d target phrases over [%d,%d]", phrases.size(), begin, begin + phrase_length));
            
            // TODO: could also compute some number of features here (e.g., non-LM ones)
            // float score_delta = context.GetScorer().transition(ant, phrases, begin, begin + phrase_length);
            
            // Future costs: remove span to be filled.
            float future_delta = future.Change(coverage, begin, begin + phrase_length);
            
            /* This associates with each span a set of hypotheses that can be extended by
             * phrases from that span. The hypotheses are wrapped in HypoState objects, which
             * augment the hypothesis score with a future cost.
             */
            Candidate cand = new Candidate(hypotheses, phrases, span, future_delta);
            targetStack.addCandidate(cand);
          }
        }
      }

      /* At this point, every vertex contains a list of all existing hypotheses that the target
       * phrases in that vertex could extend. Now we need to create the search object, which
       * implements cube pruning. There are up to O(n^2) cubes, n the size of the current stack,
       * one cube each over each span of the input. Each "cube" has two dimensions: one representing
       * the target phrases over the span, and one representing all of these incoming hypotheses.
       * We seed the chart with the best item in each cube, and then repeatedly pop and extend.
       */
      
//      System.err.println(String.format("\nBuilding cube-pruning chart for %d words", source_words));

      targetStack.search();
    }
    
    Decoder.LOG(1, String.format("Input %d: Search took %.3f seconds", sentence.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f));
    
    return createGoalNode();
  }
    
  /**
   * Enforces reordering constraints. Our version of Moses' ReorderingConstraint::Check() and
   * SearchCubePruning::CheckDistortion(). 
   * 
   * @param coverage
   * @param begin
   * @param i
   * @return
   */
  private boolean permissible(Coverage coverage, int begin, int end) {
    int firstZero = coverage.firstZero();

    if (config.reordering_limit < 0)
      return true;
    
    /* We can always start with the first zero since it doesn't create a reordering gap
     */
    if (begin == firstZero)
      return true;

    /* If a gap is created by applying this phrase, make sure that you can reach the first
     * zero later on without violating the distortion constraint.
     */
    if (end - firstZero > config.reordering_limit) {
      return false;
    }
    
    return true;
  }


  /**
   * Searches through the goal stack, calling the final transition function on each node, and then returning
   * the best item. Usually the final transition code doesn't add anything, because all features
   * have already computed everything they need to. The standard exception is language models that
   * have not yet computed their prefix probabilities (which is not the case with KenLM, the default).
   * 
   * @return
   */
  private HyperGraph createGoalNode() {
    Stack lastStack = stacks.get(sentence.length());
    
    for (Hypothesis hyp: lastStack) {
      float score = hyp.getScore();
      List<HGNode> tailNodes = new ArrayList<HGNode>();
      tailNodes.add(hyp);
      
      float finalTransitionScore = ComputeNodeResult.computeFinalCost(featureFunctions, tailNodes, 0, sentence.length(), null, sentence);

      if (null == this.end)
        this.end = new Hypothesis(null, score + finalTransitionScore, hyp, sentence.length(), null);

      HyperEdge edge = new HyperEdge(null, score + finalTransitionScore, finalTransitionScore, tailNodes, null);
      end.addHyperedgeInNode(edge);
    }
    
    return new HyperGraph(end, -1, -1, this.sentence);
  }
}
