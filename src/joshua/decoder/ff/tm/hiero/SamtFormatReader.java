package joshua.decoder.ff.tm.hiero;

import java.util.Arrays;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

public class SamtFormatReader extends GrammarReader<BilingualRule> {

	private static final Logger logger = Logger
			.getLogger(SamtFormatReader.class.getName());
	
	private static final String samtNonTerminalMarkup;
	
	private int[] nonTerminalCache;
	
	static {
		fieldDelimiter = "#";
		nonTerminalRegEx = "^@[^\\s]+";
		nonTerminalCleanRegEx = "[\\,0-9\\s]+";
		
		samtNonTerminalMarkup = "@";
		
		description = "Original SAMT format";
	}
	
	public SamtFormatReader(String grammarFile, SymbolTable vocabulary) {
		super(grammarFile, vocabulary);
		
		// TODO: should be limited to maxNTs + 1 if defined in config.
		// position 0 will never be used
		nonTerminalCache = new int[30];
	}

	// Format example:
	// @VZ-HD @APPR-DA+ART-DA minutes#@2 protokoll @1#@PP-MO+VZ-HD#0 1 1 -0 0.5 -0
	
	@Override
	protected BilingualRule parseLine(String line) {
		String[] fields = line.split(fieldDelimiter);
		if (fields.length != 4) {
			logger.severe("Rule line does not have four fields: " + line);
			logger.severe("Skipped.");
			return null;
		}

		int lhs = symbolTable.addNonterminal(adaptNonTerminalMarkup(fields[2]));

		int arity = 0;
		
		// foreign side
		String[] foreignWords = fields[0].split("\\s+");
		int[] french = new int[foreignWords.length];
		for (int i = 0; i < foreignWords.length; i++) {
			if (isNonTerminal(foreignWords[i])) {
				arity++;
				french[i] = symbolTable.addNonterminal(adaptNonTerminalMarkup(foreignWords[i], arity));
				nonTerminalCache[arity] = french[i];
			} else {
				french[i] = symbolTable.addTerminal(foreignWords[i]);
			}
		}

		// HACK: avoid source-side loop rules
		// TODO: global lookup for goal symbol id would really help here
		if ((french.length == 1) && (arity == 1)  
				&& !adaptNonTerminalMarkup(fields[2]).equals(JoshuaConfiguration.goal_symbol)) 
		{
			return null;
		}
		
		// english side
		String[] englishWords = fields[1].split("\\s+");
		int[] english = new int[englishWords.length];
		for (int i = 0; i < englishWords.length; i++) {
			if (isNonTerminal(englishWords[i])) {
				english[i] = nonTerminalCache[Integer.
						parseInt(cleanSamtNonTerminal(englishWords[i]))];
			} else {
				english[i] = symbolTable.addTerminal(englishWords[i]);
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

	protected String cleanSamtNonTerminal(String word) {
		// changes SAMT markup to Hiero-style
		return word.replaceAll(samtNonTerminalMarkup, "");
	}
	
	protected String adaptNonTerminalMarkup(String word) {
		// changes SAMT markup to Hiero-style
		return "[" + word.replaceAll(",", "_COMMA_")
			.replaceAll("\\$", "_DOLLAR_")
			.replaceAll(samtNonTerminalMarkup, "") + "]";
	}
	
	protected String adaptNonTerminalMarkup(String word, int ntIndex) {
		// changes SAMT markup to Hiero-style
		return "[" + word.replaceAll(",", "_COMMA_")
		.replaceAll("\\$", "_DOLLAR_")
		.replaceAll(samtNonTerminalMarkup, "") + "," + ntIndex + "]";
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
		StringBuffer sb = new StringBuffer();
		sb.append(symbolTable.getWord(rule.getLHS()));
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getFrench()));
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getEnglish()));
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
		sb.append(symbolTable.getWord(rule.getLHS()));
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getFrench()));
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getEnglish()));
		sb.append(" |||");
	
		return sb.toString();
	}
}
