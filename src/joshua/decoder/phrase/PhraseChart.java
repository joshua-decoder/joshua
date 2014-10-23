package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.decoder.Decoder;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.segment_file.Sentence;

/**
 * This class represents a bundle of phrase tables that have been read in,
 * reporting some stats about them. Probably could be done away with.
 */
public class PhraseChart {

  private int sentence_length;
  private int max_source_phrase_length;

  // Banded array: different source lengths are next to each other.
  private List<List<Rule>> entries;
  
  // number of translation options
  private int numOptions = 20;
  
  private List<FeatureFunction> features;

  /**
   * Create a new PhraseChart object, which represents all phrases that are
   * applicable against the current input sentence. These phrases are extracted
   * from all available grammars.
   * 
   * @param tables
   * @param source
   */
  public PhraseChart(PhraseTable[] tables, List<FeatureFunction> features, Sentence source, int num_options) {
    
    this.features = features;
    
    float startTime = System.currentTimeMillis();
   
    max_source_phrase_length = 0;
    for (int i = 0; i < tables.length; i++)
      max_source_phrase_length = Math.max(max_source_phrase_length,
          tables[i].getMaxSourcePhraseLength());
    sentence_length = source.length();

    entries = new ArrayList<List<Rule>>(sentence_length * max_source_phrase_length);
    for (int i = 0; i < sentence_length * max_source_phrase_length; i++)
      entries.add(null);

    // There's some unreachable ranges off the edge. Meh.
    for (int begin = 0; begin < sentence_length; ++begin) {
      for (int end = begin + 1; (end != sentence_length + 1)
          && (end <= begin + max_source_phrase_length); ++end) {
        if (source.hasPath(begin, end)) {
          for (PhraseTable table : tables)
            addToRange(begin, end,
                table.getPhrases(Arrays.copyOfRange(source.intSentence(), begin, end)));
        }
      }
    }
    
    System.err.println(String.format("[%d] Collecting options took %.3f seconds", source.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f));
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
//    System.err.println(String.format("PhraseChart::getRange(%d,%d): found %d entries", begin, end,
//        entries.get(index) == null ? 0 : entries.get(index).size()));

    if (index < 0 || index >= entries.size() || entries.get(index) == null)
      return null;

    TargetPhrases phrases = new TargetPhrases(entries.get(index));
    phrases.finish(features, Decoder.weights, numOptions);
    
    return phrases;
  }

  /**
   * Add a set of phrases from a grammar to the current span.
   * 
   * @param begin
   * @param end
   * @param to
   */
  private void addToRange(int begin, int end, List<Rule> to) {
    if (to != null) {
//      System.err.println(String.format("PhraseChart::addToRange(%d, %d) = %d targets", begin, end, to.size()));

      try {
        int offset = offset(begin, end);
        if (entries.get(offset) == null)
          entries.set(offset, new ArrayList<Rule>());
        entries.get(offset).addAll(to);
      } catch (java.lang.IndexOutOfBoundsException e) {
        System.err.println(String.format("Whoops! %s [%d-%d] too long (%d)", to, begin, end,
            entries.size()));
      }
    }
  }
}
