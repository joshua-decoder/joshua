package joshua.decoder.ff.tm.format;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.Rule;

/**
 * This class implements reading files in the format defined by David Chiang for Hiero. 
 * 
 * @author Unknown
 * @author Matt Post <post@cs.jhu.edu>
 */

public class HieroFormatReader extends GrammarReader<Rule> {

  static {
    fieldDelimiter = "\\s\\|{3}\\s";
    nonTerminalRegEx = "^\\[[^\\s]+\\,[0-9]*\\]$";
    nonTerminalCleanRegEx = ",[0-9\\s]+";
    // nonTerminalRegEx = "^\\[[A-Z]+\\,[0-9]*\\]$";
    // nonTerminalCleanRegEx = "[\\[\\]\\,0-9\\s]+";
    description = "Original Hiero format";
  }

  public HieroFormatReader() {
    super();
  }

  public HieroFormatReader(String grammarFile) {
    super(grammarFile);
  }

  @Override
  public Rule parseLine(String line) {
    String[] fields = line.split(fieldDelimiter);
    if (fields.length < 3) {
      throw new RuntimeException(String.format("Rule '%s' does not have four fields", line));
    }

    int lhs = Vocabulary.id(cleanNonTerminal(fields[0]));

    int arity = 0;
    // foreign side
    String[] foreignWords = fields[1].split("\\s+");
    int[] french = new int[foreignWords.length];
    for (int i = 0; i < foreignWords.length; i++) {
      french[i] = Vocabulary.id(foreignWords[i]);
      if (Vocabulary.nt(french[i])) {
        arity++;
        french[i] = cleanNonTerminal(french[i]);
      }
    }

    // English side
    String[] englishWords = fields[2].split("\\s+");
    int[] english = new int[englishWords.length];
    for (int i = 0; i < englishWords.length; i++) {
      english[i] = Vocabulary.id(englishWords[i]);
      if (Vocabulary.nt(english[i])) {
        english[i] = -Vocabulary.getTargetNonterminalIndex(english[i]);
      }
    }

    String sparse_features = (fields.length > 3 ? fields[3] : "");
    String alignment = (fields.length > 4) ? fields[4] : null;

    return new Rule(lhs, french, english, sparse_features, arity, alignment);
  }

  @Override
  public String toWords(Rule rule) {
    StringBuffer sb = new StringBuffer("");
    sb.append(Vocabulary.word(rule.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getEnglish()));
    sb.append(" |||");
    sb.append(" " + rule.getFeatureVector());

    return sb.toString();
  }

  @Override
  public String toWordsWithoutFeatureScores(Rule rule) {
    StringBuffer sb = new StringBuffer();
    sb.append(rule.getLHS());
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getEnglish()));
    sb.append(" |||");

    return sb.toString();
  }


  public static String getFieldDelimiter() {
    return fieldDelimiter;
  }

  public static boolean isNonTerminal(final String word) {
    return GrammarReader.isNonTerminal(word);
  }
}
