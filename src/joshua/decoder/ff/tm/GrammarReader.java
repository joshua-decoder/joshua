package joshua.decoder.ff.tm;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.SymbolTable;
import joshua.util.io.LineReader;

/**
 * This is a base class for simple, ASCII line-based grammars that are stored on
 * disk.
 * 
 * @author Juri Ganitkevitch
 * 
 */
public abstract class GrammarReader<R extends Rule> implements
		Iterable<R>, Iterator<R> {

	protected static String fieldDelimiter;
	protected static String nonTerminalRegEx;
	protected static String nonTerminalCleanRegEx;

	protected static String description;

	protected SymbolTable symbolTable;

	protected String fileName;
	protected LineReader reader;
	protected String lookAhead;
		
	private static final Logger logger = Logger
			.getLogger(GrammarReader.class.getName());

	// dummy constructor for 
	public GrammarReader() {
		this.symbolTable = null;
		this.fileName = null;
	}
	
	public GrammarReader(String fileName, SymbolTable symbolTable) 
	{
		this.fileName = fileName;
		this.symbolTable = symbolTable;
	}

	public void initialize() {
		try {
			this.reader = new LineReader(fileName);
		} catch (IOException e) {
			logger.severe("Error opening translation model file: " + fileName);
		}

		advanceReader();
	}

	// the reader is the iterator itself
	public Iterator<R> iterator() {
		return this;
	}

	public void remove() {
		// iterator method, do nothing
	}

	public void finalize() {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.info("Error closing grammar file stream: "
							+ fileName);
			}
			reader = null;
		}
	}

	public boolean hasNext() {
		return lookAhead != null;
	}

	private void advanceReader() {
		try {
			lookAhead = reader.readLine();
		} catch (IOException e) {
			if (logger.isLoggable(Level.SEVERE))
				logger.info("Error reading grammar from file: " + fileName);
		}
		if (lookAhead == null && reader != null)
			finalize();
	}

	public R next() {
		String line = lookAhead;
		advanceReader();
		return parseLine(line);
	}

	protected abstract R parseLine(String line);
		
	// TODO: keep these around or not?
	public abstract String toWords(R rule);
	public abstract String toWordsWithoutFeatureScores(R rule);

	public abstract String toTokenIds(R rule);
	public abstract String toTokenIdsWithoutFeatureScores(R rule);
	
	public final int cleanNonTerminal(int tokenID) {
		// cleans NT of any markup
		return symbolTable.addNonterminal(symbolTable.getWord(tokenID)
				.replaceAll(nonTerminalCleanRegEx, ""));
	}

	public final String cleanNonTerminal(String word) {
		// cleans NT of any markup
		return word.replaceAll(nonTerminalCleanRegEx, "");
	}

	public final boolean isNonTerminal(final String word) {
		// checks if word matches NT regex
		return word.matches(nonTerminalRegEx);
	}

	public String getNonTerminalRegEx() {
		return nonTerminalRegEx;
	}

	public String getNonTerminalCleanRegEx() {
		return nonTerminalCleanRegEx;
	}
}
