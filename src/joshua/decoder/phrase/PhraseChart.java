package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  /**
   * Create a new PhraseChart object, which represents all phrases that are applicable against
   * the current input sentence. These phrases are extracted from all avialable grammars.
   * 
   * @param tables
   * @param source
   */
  public PhraseChart(PhraseTable[] tables, Sentence source) {
    max_source_phrase_length = 0;
    for (int i = 0; i < tables.length; i++)
      max_source_phrase_length = Math.max(max_source_phrase_length,
          tables[i].getMaxSourcePhraseLength());
    sentence_length = source.length();
    
    System.err.println(String.format("PhraseChart()::Initializing chart of size %d max %d from %s",
        sentence_length, max_source_phrase_length, source));

    entries = new ArrayList<TargetPhrases>();
    for (int i = 0; i < sentence_length * max_source_phrase_length; i++)
      entries.add(null);

    // There's some unreachable ranges off the edge. Meh.
    for (int begin = 0; begin != sentence_length; ++begin) {
      for (int end = begin + 1; (end != sentence_length + 1)
          && (end <= begin + max_source_phrase_length); ++end) {
        if (source.hasPath(begin, end))
          for (PhraseTable table : tables)
            SetRange(begin, end,
                table.Phrases(Arrays.copyOfRange(source.intSentence(), begin, end)));
      }

      /*
       * // TODO: add passthrough grammar! if (Range(begin, begin + 1) == null)
       * { // Add passthrough for words not known to the phrase table.
       * TargetPhrases passThrough = new TargetPhrases();
       * passThrough.MakePassThrough(scorer, source.intSentence()[begin]);
       * SetRange(begin, begin + 1, passThrough); }
       */
    }
  }

  public int SentenceLength() {
    return sentence_length;
  }

  // c++: TODO: make this reflect the longest source phrase for this sentence.
  public int MaxSourcePhraseLength() {
    return max_source_phrase_length;
  }

  public TargetPhrases Range(int begin, int end) {
    int index = begin * max_source_phrase_length + end - begin - 1;
    if (index < 0 || index >= entries.size() || entries.get(index) == null)
      return null;

    System.err.println(String.format("TargetPhrases::Range(%d,%d) = '%s'", begin, end,
        entries.get(index)));
    return entries.get(index);
  }

  private void SetRange(int begin, int end, RuleCollection to) {
    if (to != null) {
      try {
        System.err.println(String.format("Chart::SetRange(%d,%d) = %s", begin, end, to));
        int offset = begin * max_source_phrase_length + end - begin - 1;
        if (entries.get(offset) == null)
          entries.set(offset, new TargetPhrases(to.getRules()));
        else
          entries.get(offset).extend(to);
      } catch (java.lang.IndexOutOfBoundsException e) {
        System.err.println(String.format("Whoops! %s [%d-%d] too long (%d)", to, begin, end,
            entries.size()));
      }
    }
  }
}
