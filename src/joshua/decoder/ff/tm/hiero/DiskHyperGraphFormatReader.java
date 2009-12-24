package joshua.decoder.ff.tm.hiero;

import java.util.Arrays;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

/**
 * this class implements the grammar reader for disk hypergraph
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-11-30 23:52:12 -0500 (星期一, 30 十一月 2009) $
 */

public class DiskHyperGraphFormatReader extends GrammarReader<BilingualRule> {

	protected static final String blockDelimiter;
	
	private static final Logger logger = Logger
			.getLogger(DiskHyperGraphFormatReader.class.getName());

	static {
		blockDelimiter = " -LZF- ";
		fieldDelimiter = "\\s+\\|{3}\\s+";
		nonTerminalRegEx = "^\\[[A-Z]+\\,[0-9]*\\]$";
		nonTerminalCleanRegEx = "[\\,0-9\\s]+";
		//nonTerminalCleanRegEx = "[\\[\\]\\,0-9]+";
		description = "Joshua hypergraph rule file format";
	}
	
	public DiskHyperGraphFormatReader(String grammarFile, SymbolTable symbolTable) {
		super(grammarFile, symbolTable);
	}

	@Override
	protected BilingualRule parseLine(String line) {
		// line format: ruleID owner RULE_TBL_SEP rule
		//13137 pt -LZF- [X] ||| [X,1] a ||| [X,1] the ||| 1.6320 2.5629 0.7996

		
		String[] blocks = line.split(blockDelimiter);
		if (blocks.length != 2) {
			logger.severe("Rule line does not have two fields: " + line);
		}
		
		String[] header = blocks[0].split("\\s+");
		
		int id = Integer.parseInt(header[0]);
		int owner = symbolTable.addTerminal(header[1]);
		
		
		
		String[] fields = blocks[1].split(fieldDelimiter);
		if (fields.length < 4) {
			logger.severe("Rule line does not have four fields: " + line);
		}

		int lhs = symbolTable.addNonterminal(cleanNonTerminal(fields[0]));
		//System.out.println("original="+ fields[0] +"; lhs=" + cleanNonTerminal(fields[0]) + "; id=" +lhs);System.exit(1);
		
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

		// english side
		String[] englishWords = fields[2].split("\\s+");
		int[] english = new int[englishWords.length];
		for (int i = 0; i < englishWords.length; i++) {
			if (isNonTerminal(englishWords[i])) {
				english[i] = symbolTable.addNonterminal(englishWords[i]);
			} else {
				english[i] = symbolTable.addTerminal(englishWords[i]);
			}
		}

		// feature scores
		String[] scores = fields[3].split("\\s+");
		
		float[] feature_scores = new float[scores.length];
		for (int i = 0; i < scores.length; i++) {
			feature_scores[i] = Float.parseFloat(scores[i]);
		}
		
		return new BilingualRule(lhs, french, english, 
				feature_scores, arity, owner, 0, id);
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

		sb.append(rule.getRuleID());
		sb.append(' ');
		sb.append(symbolTable.getWord(rule.getOwner()));
		sb.append(blockDelimiter);
		
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
	
		return sb.toString();
	}
}
