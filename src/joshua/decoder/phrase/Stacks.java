package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.Span;
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

  private PhraseChart chart;

  private JoshuaConfiguration config;

  /**
   * 
   * 
   * @param context
   * @param chart
   * @param featureFunctions
   */
//  public Stacks(Context context, Chart chart, List<FeatureFunction> featureFunctions) {
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
    
    phraseTables[phraseTables.length - 2] = new PhraseTable("null", config, featureFunctions);
    phraseTables[phraseTables.length - 2].addEOSRule();
    
    phraseTables[phraseTables.length - 1] = new PhraseTable("oov", config, featureFunctions);
    AbstractGrammar.addOOVRules(phraseTables[phraseTables.length - 1], sentence.intLattice(), featureFunctions, config.true_oovs_only);
    
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
    
    // Reservation is critical because pointers to Hypothesis objects are retained as history.
    //stacks.reserve(chart.SentenceLength() + 2 /* begin/end of sentence */);
    stacks.clear();
    stacks.add(null);
    for (int i = 1; i <= sentence.length(); i++)
      stacks.add(new Stack());

    // Initialize root hypothesis with <s> context and future cost for everything.
    ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, Hypothesis.BEGIN_RULE,
        null, -1, 1, null, this.sentence);
    stacks.get(1).add(new Hypothesis(result.getDPStates(), future.Full()));
    
    // Decode with increasing numbers of source words.
    for (int source_words = 2; source_words <= sentence.length(); ++source_words) {
      // A vertex represents the root of a trie, e.g., bundled translations of the same source phrase
      HashMap<Span, Vertex> vertices = new HashMap<Span, Vertex>();
      // Iterate over stacks to continue from.
      for (int from_stack = source_words - Math.min(source_words - 1, chart.MaxSourcePhraseLength());
           from_stack < source_words;
           ++from_stack) {
        int phrase_length = source_words - from_stack;

//        System.err.println(String.format("\n  WORDS %d (STACK %d phrase_length %d)", source_words, from_stack, phrase_length));
        
        // Iterate over antecedents in this stack.
        for (Hypothesis ant : stacks.get(from_stack)) {
//          System.err.println(String.format("  WORDS %d ANT %s", source_words, ant)); 
          Coverage coverage = ant.GetCoverage();
          int begin = coverage.firstZero();
          int last_end = Math.min(coverage.firstZero() + config.reordering_limit, chart.SentenceLength());
          int last_begin = (last_end > phrase_length) ? (last_end - phrase_length) : 0;

          // We can always go from first_zero because it doesn't create a reordering gap.
          do {
            
            // Don't append </s> until the end
            if (begin == sentence.length() - 1 && source_words != sentence.length()) 
              continue;            

            TargetPhrases phrases = chart.getRange(begin, begin + phrase_length);
            
            if (phrases == null || !coverage.compatible(begin, begin + phrase_length))
              continue;

//            System.err.println(String.format("  Applying %d target phrases over [%d,%d]", phrases.size(), begin, begin + phrase_length));
            
            // TODO: could also compute some number of features here (e.g., non-LM ones)
            // float score_delta = context.GetScorer().transition(ant, phrases, begin, begin + phrase_length);
            
            // Future costs: remove span to be filled.
            float score_delta = future.Change(coverage, begin, begin + phrase_length);
            
            Span span = new Span(begin, begin + phrase_length);
            if (! vertices.containsKey(span))
              vertices.put(span, new Vertex());
            
            /* This associates with each span a set of hypotheses that can be extended by
             * phrases from that span. The hypotheses are wrapped in HypoState objects, which
             * augment the hypothesis score with a future cost.
             */
            vertices.get(span).add(new HypoState(ant, score_delta));

            // Enforce the reordering limit on later iterations.

          } while (++begin <= last_begin);
        }
      }

      /* At this point, every vertex contains a list of all existing hypotheses that the target
       * phrases in that vertex could extend. Now we need to create the search object, which
       * implements cube pruning. There are up to O(n^2) cubes, n the size of the current stack,
       * one cube each over each span of the input. Each "cube" has two dimensions: one representing
       * the target phrases over the span, and one representing all of these incoming hypotheses.
       * We seed the chart with the best item in each cube, and then repeatedly pop and extend.
       */
      
      EdgeGenerator gen = new EdgeGenerator(sentence, featureFunctions, config);
//      System.err.println(String.format("\nBuilding cube-pruning chart for %d words", source_words));
      for (Span pair : vertices.keySet()) {
        Vertex hypos = vertices.get(pair);
        if (hypos.isEmpty())
          continue;
        // Sorts the hypotheses, since we now know that we're done adding them
        hypos.finish();
        
        TargetPhrases phrases = chart.getRange(pair.start, pair.end);

//        System.err.println(String.format("  Span %s hypotheses %s phrases %s", pair, hypos.size(), phrases.size()));

        Candidate cand = new Candidate(hypos, phrases, pair);
        gen.AddCandidate(cand);
      }

      Stack stack = stacks.get(source_words);
      EdgeOutput output = new EdgeOutput(stack);
      gen.Search(output);
    }
    
    System.err.println(String.format("[%d] Search took %.3f seconds", sentence.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f));
    
    //    System.err.println("Stack(): END: " + end);
    return createGoalNode();
  }
    
  private HyperGraph createGoalNode() {
    Stack lastStack = stacks.get(sentence.length());
    
    for (Hypothesis hyp: lastStack) {
      float score = hyp.getScore();
      List<HGNode> tailNodes = new ArrayList<HGNode>();
      tailNodes.add(hyp);
      
      float finalTransitionScore = ComputeNodeResult.computeFinalCost(featureFunctions, tailNodes, 0, sentence.length(), null, sentence.id());

      if (null == this.end)
        this.end = new Hypothesis(null, score + finalTransitionScore, hyp, sentence.length(), null);

      HyperEdge edge = new HyperEdge(null, score + finalTransitionScore, finalTransitionScore, tailNodes, null);
      end.addHyperedgeInNode(edge);
    }
    
    this.end = lastStack.isEmpty() ? null : lastStack.get(0);
    
    return new HyperGraph(end, -1, -1, this.sentence);
  }

  
  /**
   * Creates a new hypothesis and adds it to the stack.
   * 
   * Duplication-checking is done elsewhere. For regular decoding, an {@link EdgeOutput} keeps
   * track of added edges and combines the last item, after this call completes.
   * 
   * @param complete the candidate used to build the hypothesis
   * @param out the stack to place it on
   */
  public static void AppendToStack(Candidate complete, Stack out) {
    
    Hypothesis h = new Hypothesis(complete);
    out.add(h);

    /*
    out.add(new Hypothesis(0, // complete.CompletedState().right,
          complete.GetScore(), // TODO: call scorer to adjust for last of lexro?
          (Hypothesis)(complete.NT()[0].End().get()),
          source_range.start,
          source_range.end,
          (Phrase)complete.NT()[1].End().get()));
     */
//    out.add(h);
  }
}
