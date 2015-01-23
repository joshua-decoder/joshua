package joshua.decoder.phrase;

import java.util.ArrayList;	
import java.util.Arrays;
import java.util.List;

import joshua.decoder.Decoder;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.segment_file.Sentence;

/**
 * This class represents a bundle of phrase tables that have been read in,
 * reporting some stats about them. Probably could be done away with.
 */
public class PhraseChart {

  private int sentence_length;
  private int max_source_phrase_length;

  // Banded array: different source lengths are next to each other.
  private List<TargetPhrases> entries;

  // number of translation options
  int numOptions = 20;
  private List<FeatureFunction> features;

  /**
   * Create a new PhraseChart object, which represents all phrases that are
   * applicable against the current input sentence. These phrases are extracted
   * from all available grammars.
   * 
   * @param tables
   * @param source
   */
  public PhraseChart(PhraseTable[] tables, List<FeatureFunction> features, Sentence source,
      int num_options) {

    float startTime = System.currentTimeMillis();

    this.numOptions = num_options;
    this.features = features;

    max_source_phrase_length = 0;
    for (int i = 0; i < tables.length; i++)
      max_source_phrase_length = Math.max(max_source_phrase_length,
          tables[i].getMaxSourcePhraseLength());
    sentence_length = source.length();

//    System.err.println(String.format(
//        "PhraseChart()::Initializing chart for sentlen %d max %d from %s", sentence_length,
//        max_source_phrase_length, source));

    entries = new ArrayList<TargetPhrases>();
    for (int i = 0; i < sentence_length * max_source_phrase_length; i++)
      entries.add(null);

    // There's some unreachable ranges off the edge. Meh.
    for (int begin = 0; begin != sentence_length; ++begin) {
      for (int end = begin + 1; (end != sentence_length + 1)
          && (end <= begin + max_source_phrase_length); ++end) {
        if (source.hasPath(begin, end)) {
          for (PhraseTable table : tables)
            addToRange(begin, end,
                table.getPhrases(Arrays.copyOfRange(source.getWordIDs(), begin, end)));
        }

      }
    }

    for (TargetPhrases phrases : entries) {
      if (phrases != null)
        phrases.finish(features, Decoder.weights, num_options);
    }

    Decoder.LOG(1, String.format("Input %d: Collecting options took %.3f seconds", source.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f));
    
    if (Decoder.VERBOSE(3)) {
      for (int i = 1; i < sentence_length - 1; i++) {
        for (int j = i + 1; j < sentence_length && j <= i + max_source_phrase_length; j++) {
          if (source.hasPath(i, j)) {
            TargetPhrases phrases = getRange(i, j);
            if (phrases != null) {
              System.err.println(String.format("%s (%d-%d)", source.source(i,j), i, j));
              for (Rule rule: phrases)
                System.err.println(String.format("    %s :: est=%.3f", rule.getEnglishWords(), rule.getEstimatedCost()));
            }
          }
        }
      }
    }
  }

  public int SentenceLength() {
    return sentence_length;
  }

  // c++: TODO: make this reflect the longest source phrase for this sentence.
  public int MaxSourcePhraseLength() {
    return max_source_phrase_length;
  }

  /**
   * Maps two-dimensional span into a one-dimensional array.
   * 
   * @param i
   * @param j
   * @return offset into private list of TargetPhrases
   */
  private int offset(int i, int j) {
    return i * max_source_phrase_length + j - i - 1;
  }

  /**
   * Returns phrases from all grammars that match the span.
   * 
   * @param begin
   * @param end
   * @return
   */
  public TargetPhrases getRange(int begin, int end) {
    int index = offset(begin, end);
    // System.err.println(String.format("PhraseChart::Range(%d,%d): found %d entries",
    // begin, end,
    // entries.get(index) == null ? 0 : entries.get(index).size()));
    // if (entries.get(index) != null)
    // for (Rule phrase: entries.get(index))
    // System.err.println("  RULE: " + phrase);

    if (index < 0 || index >= entries.size() || entries.get(index) == null)
      return null;

    return entries.get(index);
  }

  /**
   * Add a set of phrases from a grammar to the current span.
   * 
   * @param begin
   * @param end
   * @param to
   */
  private void addToRange(int begin, int end, RuleCollection to) {
    if (to != null) {
      /*
       * This first call to getSortedRules() is important, because it is what
       * causes the scoring and sorting to happen. It is also a synchronized call,
       * which is necessary because the underlying grammar gets sorted. Subsequent calls to get the
       * rules will just return the already-sorted list. Here, we score, sort,
       * and then trim the list to the number of translation options. Trimming provides huge
       * performance gains --- the more common the word, the more translations options it is
       * likely to have (often into the tens of thousands).
       */
      List<Rule> rules = to.getSortedRules(features);
      if (numOptions > 0 && rules.size() > numOptions)
        rules = rules.subList(0,  numOptions);
//        to.getRules().subList(numOptions, to.getRules().size()).clear();

      try {
        int offset = offset(begin, end);
        if (entries.get(offset) == null)
          entries.set(offset, new TargetPhrases(rules));
        else
          entries.get(offset).addAll(rules);
      } catch (java.lang.IndexOutOfBoundsException e) {
        System.err.println(String.format("Whoops! %s [%d-%d] too long (%d)", to, begin, end,
            entries.size()));
      }
    }
  }
}
