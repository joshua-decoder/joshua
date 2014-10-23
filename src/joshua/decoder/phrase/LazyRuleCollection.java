package joshua.decoder.phrase;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.format.HieroFormatReader;

public class LazyRuleCollection extends BasicRuleCollection {

  private List<String> ruleStrings;
  private int lhs;
  private int owner;

  /**
   * Constructs an initially empty rule collection.
   * 
   * @param arity Number of nonterminals in the source pattern
   * @param sourceTokens Sequence of terminals and nonterminals in the source
   *          pattern
   */
  public LazyRuleCollection(int owner, int arity, int[] sourceTokens) {
    super(arity, sourceTokens);

    this.owner = owner;
    this.lhs = Vocabulary.id("[X]");
  }

  public LazyRuleCollection(int owner, int arity, int[] sourceTokens, List<String> targetSides) {
    super(arity, sourceTokens);

    this.owner = owner;
    this.ruleStrings = targetSides;
    this.lhs = Vocabulary.id("[X]");
    
//    System.err.println(String.format("LazyRuleCollection(%s): created new with %d", Vocabulary.getWords(sourceTokens),
//        targetSides.size()));
  }

  static String fieldDelimiter = "\\s+\\|{3}\\s+";

  /**
   * This function transforms the unprocessed strings (read from the text file)
   * into {@link BilingualRule} objects. These have not yet been scored.
   */
  public List<Rule> getRules() {
    if (ruleStrings.size() > rules.size()) {
      for (String line : ruleStrings) {
        String[] fields = line.split(fieldDelimiter);

        // foreign side
        int[] french = new int[sourceTokens.length + 1];
        french[0] = lhs;
        System.arraycopy(sourceTokens, 0, french, 1, sourceTokens.length);

        // English side
        String[] englishWords = fields[0].split("\\s+");
        int[] english = new int[englishWords.length + 1];
        english[0] = -1;
        for (int i = 0; i < englishWords.length; i++) {
          english[i + 1] = Vocabulary.id(englishWords[i]);
        }

        // transform feature values
        StringBuffer values = new StringBuffer();
        for (String value : fields[1].split(" ")) {
          float f = Float.parseFloat(value);
          values.append(String.format("%f ", f <= 0.0 ? -100 : -Math.log(f)));
        }
        String sparse_features = values.toString().trim();

        // alignments
        byte[] alignment = null;
        if (fields.length > 3) { // alignments are included
          alignment = HieroFormatReader.readAlignment(fields[2]);
        } else {
          alignment = null;
        }

        // System.out.println(String.format("parseLine: %s\n  ->%s", line,
        // sparse_features));

        BilingualRule rule = new BilingualRule(lhs, french, english, sparse_features, arity,
            alignment);
        rule.setOwner(owner);
        rules.add(rule);
      }
    }

    return this.rules;
  }

  public boolean isSorted() {
    return sorted;
  }
}
