package joshua.discriminative.monolingual_parser;

import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.MonolingualRule;
import joshua.decoder.ff.tm.hiero.HieroFormatReader;

public class MonolingualGrammarReader extends GrammarReader<MonolingualRule> {
	boolean addFakeFeatScoreForEM = false;

	private static final Logger logger = Logger
	.getLogger(HieroFormatReader.class.getName());

	static {
	fieldDelimiter = "\\s+\\|{3}\\s+";
	nonTerminalRegEx = "^\\[[A-Z]+\\,[0-9]*\\]$";
	//nonTerminalCleanRegEx = "[\\[\\]\\,0-9]+";
	nonTerminalCleanRegEx = "[\\,0-9\\s]+";
	description = "Original monolingual format";
	}
	
	
	public MonolingualGrammarReader(String grammarFile, SymbolTable vocabulary,
			boolean addFakeFeatScoreForEM_) {
		super(grammarFile, vocabulary);
		this.addFakeFeatScoreForEM = addFakeFeatScoreForEM_;
	}
	
	
	@Override
	protected MonolingualRule parseLine(String line) {
		String[] fields = line.split(fieldDelimiter);
		if (fields.length != 3) {
			logger.severe("Rule line does not have four fields: " + line);
		}

		int lhs = symbolTable.addNonterminal(cleanNonTerminal(fields[0]));

		int arity = 0;
		// foreign side
		String[] foreignWords = fields[1].split("\\s+");
		int[] french = new int[foreignWords.length];
		for (int i = 0; i < foreignWords.length; i++) {
			if (isNonTerminal(foreignWords[i])) {
				arity++;
				french[i] = symbolTable.addNonterminal(foreignWords[i]);
			} else {
				french[i] = symbolTable.addTerminal(foreignWords[i]);
			}
		}

		// feature scores
		String[] scores = fields[2].split("\\s+");		
		float[] feature_scores;
		if(addFakeFeatScoreForEM)
			feature_scores = new float[scores.length+1];
		else
			feature_scores = new float[scores.length];
		
		int i = 0;
		for (String score : scores) {
			feature_scores[i++] = Float.parseFloat(score);
		}

		//?????????????? res.estimateRuleCost(p_l_models);//estimate lower-bound, and set statelesscost, this must be called
		return new MonolingualRule(lhs, french, feature_scores, arity);
	}


	@Override
	public String toTokenIds(MonolingualRule rule) {
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(rule.getFrench());
		sb.append(" |||");

		float[] feature_scores = rule.getFeatureScores();
		for (int i = 0; i < feature_scores.length; i++) {
			sb.append(String.format(" %.4f", feature_scores[i]));
		}
		return sb.toString();
	}

	@Override
	public String toTokenIdsWithoutFeatureScores(MonolingualRule rule) {
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(rule.getFrench());
		return sb.toString();
	}

	@Override
	public String toWords(MonolingualRule rule) {
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getFrench()));
		sb.append(" ||| ");

		float[] feature_scores = rule.getFeatureScores();
		for (int i = 0; i < feature_scores.length; i++) {
			sb.append(String.format(" %.4f", feature_scores[i]));
		}
		return sb.toString();
	}

	@Override
	public String toWordsWithoutFeatureScores(MonolingualRule rule) {	
		StringBuffer sb = new StringBuffer();
		sb.append(rule.getLHS());
		sb.append(" ||| ");
		sb.append(symbolTable.getWords(rule.getFrench()));
		return sb.toString();
	}

}
