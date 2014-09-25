package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.Span;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
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
    
    PhraseTable[] phraseTables = new PhraseTable[num_phrase_tables];
    for (int i = 0, j = 0; i < grammars.length; i++)
      if (grammars[i] instanceof PhraseTable)
        phraseTables[j++] = (PhraseTable) grammars[i];
    
    this.chart = new PhraseChart(phraseTables, sentence);
  }
  
  
  /**
   * The main algorithm. Returns a hypergraph representing the search space.
   * 
   * @return
   */
  public HyperGraph search() {
    Future future = new Future(chart);
    stacks = new ArrayList<Stack>();
    
    // Reservation is critical because pointers to Hypothesis objects are retained as history.
    //stacks.reserve(chart.SentenceLength() + 2 /* begin/end of sentence */);
    stacks.clear();
    for (int i = 0; i < sentence.length(); i++)
      stacks.add(new Stack());

    // Initialize root hypothesis with <s> context and future cost for everything.
    stacks.get(0).add(new Hypothesis(null, future.Full()));

    // Decode with increasing numbers of source words.
    for (int source_words = 1; source_words <= sentence.length(); ++source_words) {
      // A vertex represents the root of a trie, e.g., bundled translations of the same source phrase
      HashMap<Span, Vertex> vertices = new HashMap<Span, Vertex>();
      // Iterate over stacks to continue from.
      for (int from_stack = source_words - Math.min(source_words, chart.MaxSourcePhraseLength());
           from_stack < source_words;
           ++from_stack) {
        int phrase_length = source_words - from_stack;

        // Iterate over antecedents in this stack.
        for (Hypothesis ant : stacks.get(from_stack)) {
          System.err.println(ant);
          Coverage coverage = ant.GetCoverage();
          int begin = coverage.firstZero();
          int last_end = Math.min(coverage.firstZero() + config.reordering_limit, chart.SentenceLength());
          int last_begin = (last_end > phrase_length) ? (last_end - phrase_length) : 0;

          // TODO: don't consume </s> until you're on the last stack
          
          // We can always go from first_zero because it doesn't create a reordering gap.
          do {
            Span span = new Span(begin, begin + phrase_length);
            TargetPhrases phrases = chart.Range(begin, begin + phrase_length);
            
            System.err.println(String.format("  Applying target phrases over [%d,%d] = %s", begin, begin + phrase_length, phrases));
            if (phrases == null || !coverage.compatible(begin, begin + phrase_length)) {
              System.err.println(String.format("  - no phrases (%s) or incompatible coverage (%s)", phrases, coverage.compatible(begin, begin+ phrase_length)));
              continue;
            }
            
            // TODO: could also compute some number of features here (e.g., non-LM ones)
            // float score_delta = context.GetScorer().transition(ant, phrases, begin, begin + phrase_length);
            
            // Future costs: remove span to be filled.
            float score_delta = future.Change(coverage, begin, begin + phrase_length);
            
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
      for (Span pair : vertices.keySet()) {
        Vertex hypos = vertices.get(pair);
        if (hypos.isEmpty())
          continue;
        
        // Sorts the hypotheses, since we now know that we're done adding them
        hypos.finish();

        TargetPhrases phrases = chart.Range(pair.start, pair.end);
        Candidate candidate = new Candidate(hypos, phrases, pair);
        gen.AddCandidate(candidate);
      }

      //stacks.resize(stacks_.size() + 1); // todo
      //stacks.back().reserve(context.SearchContext().PopLimit()); todo
      Stack lastStack = stacks.get(stacks.size()-1);
      EdgeOutput output = new EdgeOutput(lastStack);
      gen.Search(output);
    }

    return PopulateLastStack();
  }

  /**
   * The last stack is a bit of a special case, because only the last word of the input
   * sentence (</s>) can be translated --- not just any word.
   * 
   * I think this function could be done away with entirely in favor of a little special-case
   * handling in the above routine.
   * 
   * @return
   */
  private HyperGraph PopulateLastStack() {
    
    Span span = new Span(1, chart.SentenceLength());
    Vertex all_hyps = new Vertex();
    
    for (Hypothesis ant : stacks.get(chart.SentenceLength())) {
      // TODO: the zero in the following line assumes that EOS is not scored for distortion. 
      // This assumption might need to be revisited.
      all_hyps.add(new HypoState(ant, 0));
    }
    
    // Next, make Vertex which consists of a single EOS phrase.
    // The search algorithm will attempt to find the best hypotheses in the "cross product" of these two sets.
    // TODO: Maybe this should belong to the phrase table.  It's constant.
    Phrase eos_phrase = new Phrase("</s>");
    TargetPhrases eos_phrases = new TargetPhrases();
    eos_phrases.add(eos_phrase);
    eos_phrases.finish();

    Stack lastStack = stacks.get(stacks.size() - 1);
    
    EdgeGenerator gen = new EdgeGenerator(sentence, featureFunctions, config);
    Candidate candidate = new Candidate(all_hyps, eos_phrases, span);
    gen.AddCandidate(candidate);

    PickBest output = new PickBest(lastStack);
    gen.Search(output);

    end = lastStack.isEmpty() ? null : lastStack.get(0);
    
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
