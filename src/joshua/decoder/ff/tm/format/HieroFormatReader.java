package joshua.decoder.ff.tm.format;

import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

/**
 * This class implements reading files in the format defined by David Chiang for Hiero. 
 * 
 * @author Unknown
 * @author Matt Post <post@cs.jhu.edu>
 */

public class HieroFormatReader extends GrammarReader<BilingualRule> {

  private static final Logger logger = Logger.getLogger(HieroFormatReader.class.getName());

  static {
    fieldDelimiter = "\\s+\\|{3}\\s+";
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
  public BilingualRule parseLine(String line) {
    String[] fields = line.split(fieldDelimiter);
    if (fields.length < 4) {
      logger.severe("Rule line does not have four fields: " + line);
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

    // english side
    String[] englishWords = fields[2].split("\\s+");
    int[] english = new int[englishWords.length];
    for (int i = 0; i < englishWords.length; i++) {
      english[i] = Vocabulary.id(englishWords[i]);
      if (Vocabulary.nt(english[i])) {
        english[i] = -Vocabulary.getTargetNonterminalIndex(english[i]);
      }
    }

    String sparse_features = fields[3];

    byte[] alignment = null;
    if (fields.length > 4) { // alignments are included
      alignment = readAlignment(fields[4]);
    } else {
      alignment = null;
    }

    return new BilingualRule(lhs, french, english, sparse_features, arity, alignment);
  }

	private static byte [] readAlignment(String s) {
		String [] indices = s.replaceAll("-", " ").split("\\s+");
		byte [] result = new byte[indices.length];
		int j = 0;
		for (String i : indices) {
			try {
				result[j] = Byte.parseByte(i);
			} catch (NumberFormatException e) {
				return null; // malformed alignment; just ignore it.
			}
			j++;
		}
		return result;
	}

  @Override
  public String toWords(BilingualRule rule) {
    StringBuffer sb = new StringBuffer("");
    sb.append(Vocabulary.word(rule.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(rule.getEnglish()));
    sb.append(" |||");
    sb.append(" " + rule.computeFeatures());

    return sb.toString();
  }

  @Override
  public String toWordsWithoutFeatureScores(BilingualRule rule) {
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
