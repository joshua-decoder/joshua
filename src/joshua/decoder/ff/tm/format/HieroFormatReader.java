package joshua.decoder.ff.tm.format;

import java.util.Arrays;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

public class HieroFormatReader extends GrammarReader<BilingualRule> {

	private static final Logger logger = Logger
			.getLogger(HieroFormatReader.class.getName());
	
	static {
		fieldDelimiter = "\\s\\|{3}\\s";
		nonTerminalRegEx = "^\\[[^\\s]+\\,[0-9]*\\]$";
		nonTerminalCleanRegEx = ",[0-9\\s]+";
//		nonTerminalRegEx = "^\\[[A-Z]+\\,[0-9]*\\]$";
//		nonTerminalCleanRegEx = "[\\[\\]\\,0-9\\s]+";
		description = "Original Hiero format";
	}
	
	
	public HieroFormatReader(String grammarFile) {
		super(grammarFile);
	}

	@Override
	protected BilingualRule parseLine(String line) {
		String[] fields = line.split(fieldDelimiter);
		if (fields.length != 4) {
			logger.severe("Rule line does not have four fields: " + line);
		}
		
		int lhs = Vocabulary.id(cleanNonTerminal(fields[0]));

		int arity = 0;
		// foreign side
		String[] foreignWords = fields[1].split("\\s+");
		int[] french = new int[foreignWords.length];
		for (int i = 0; i < foreignWords.length; i++) {
			french[i] = Vocabulary.id(foreignWords[i]);
			if (Vocabulary.nt(french[i]))
				arity++;
		}

		// english side
		String[] englishWords = fields[2].split("\\s+");
		int[] english = new int[englishWords.length];
		for (int i = 0; i < englishWords.length; i++) {
			english[i] = Vocabulary.id(englishWords[i]);
			if (Vocabulary.nt(english[i])) {
				english[i] = - Vocabulary.getTargetNonterminalIndex(english[i]);
			}
		}

		// feature scores
		String[] scores = fields[3].split("\\s+");
		float[] feature_scores = new float[scores.length];
		
		int i = 0;
		for (String score : scores) {
			feature_scores[i++] = Float.parseFloat(score);
		}
		
		return new BilingualRule(lhs, french, english, feature_scores, arity);
	}


	@Override
	public String toTokenIds(BilingualRule rule) {
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(Arrays.toString(rule.getFrench()));
		sb.append(" ||| ");
		sb.append(Arrays.toString(rule.getEnglish()));
		sb.append(" |||");

		float[] feature_scores = rule.getFeatureScores();
		for (int i = 0; i < feature_scores.length; i++) {
			sb.append(String.format(" %.4f", feature_scores[i]));
		}
		return sb.toString();
	}

	@Override
	public String toTokenIdsWithoutFeatureScores(BilingualRule rule) {
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(Arrays.toString(rule.getFrench()));
		sb.append(" ||| ");
		sb.append(Arrays.toString(rule.getEnglish()));
		return sb.toString();
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

		float[] feature_scores = rule.getFeatureScores();
		for (int i = 0; i < feature_scores.length; i++) {
			sb.append(String.format(" %.4f", feature_scores[i]));
		}
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
