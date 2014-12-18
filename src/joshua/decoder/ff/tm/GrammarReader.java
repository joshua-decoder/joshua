package joshua.decoder.ff.tm;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.util.io.LineReader;

/**
 * This is a base class for simple, ASCII line-based grammars that are stored on disk.
 * 
 * @author Juri Ganitkevitch
 * 
 */
public abstract class GrammarReader<R extends Rule> implements Iterable<R>, Iterator<R> {

  protected static String fieldDelimiter;
  protected static String nonTerminalRegEx;
  protected static String nonTerminalCleanRegEx;

  protected static String description;

  protected String fileName;
  protected LineReader reader;
  protected String lookAhead;
  protected int numRulesRead;

  private static final Logger logger = Logger.getLogger(GrammarReader.class.getName());

  // dummy constructor for
  public GrammarReader() {
    this.fileName = null;
  }

  public GrammarReader(String fileName) {
    this.fileName = fileName;
  }

  public void initialize() {
    try {
      this.reader = new LineReader(fileName);
    } catch (IOException e) {
      throw new RuntimeException("Error opening translation model file: " + fileName + "\n"
          + (null != e.getMessage() ? e.getMessage() : "No details available. Sorry."), e);
    }

    Decoder.LOG(1, String.format("Reading grammar from file %s...", fileName));
    numRulesRead = 0;
    advanceReader();
  }

  // the reader is the iterator itself
  public Iterator<R> iterator() {
    return this;
  }

  /** Unsupported Iterator method. */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void close() {
    if (null != this.reader) {
      try {
        this.reader.close();
      } catch (IOException e) {
        // FIXME: is this the right logging level?
        if (logger.isLoggable(Level.WARNING))
          logger.info("Error closing grammar file stream: " + this.fileName);
      }
      this.reader = null;
    }
  }

  /**
   * For correct behavior <code>close</code> must be called on every GrammarReader, however this
   * code attempts to avoid resource leaks.
   * 
   * @see joshua.util.io.LineReader
   */
  @Override
  protected void finalize() throws Throwable {
    if (this.reader != null) {
      logger.severe("Grammar file stream was not closed, this indicates a coding error: "
          + this.fileName);
    }

    this.close();
    super.finalize();
  }

  @Override
  public boolean hasNext() {
    return lookAhead != null;
  }

  private void advanceReader() {
    try {
      lookAhead = reader.readLine();
      numRulesRead++;
    } catch (IOException e) {
      logger.severe("Error reading grammar from file: " + fileName);
    }
    if (lookAhead == null && reader != null) {
      this.close();
    }
  }

  /**
   * Read the next line, and print reader progress.
   */
  @Override
  public R next() {
    String line = lookAhead;

    int oldProgress = reader.progress();
    advanceReader();
    
    if (Decoder.VERBOSE >= 1) {
      int newProgress = (reader != null) ? reader.progress() : 100;

      if (newProgress > oldProgress) {
        for (int i = oldProgress + 1; i <= newProgress; i++)
          if (i == 97) {
            System.err.print("1");
          } else if (i == 98) {
            System.err.print("0");
          } else if (i == 99) {
            System.err.print("0");
          } else if (i == 100) {
            System.err.println("%");
          } else if (i % 10 == 0) {
            System.err.print(String.format("%d", i));
            System.err.flush();
          } else if ((i - 1) % 10 == 0)
            ; // skip at 11 since 10, 20, etc take two digits
          else {
            System.err.print(".");
            System.err.flush();
          }
      }
    }
    return parseLine(line);
  }

  protected abstract R parseLine(String line);

  // TODO: keep these around or not?
  public abstract String toWords(R rule);

  public abstract String toWordsWithoutFeatureScores(R rule);

  /**
   * Removes square brackets (and index, if present) from nonterminal id 
   * @param tokenID
   * @return cleaned ID
   */
  public static int cleanNonTerminal(int tokenID) {
    // cleans NT of any markup, e.g., [X,1] may becomes [X], depending
    return Vocabulary.id(cleanNonTerminal(Vocabulary.word(tokenID)));
  }

  /**
   * Removes square brackets (and index, if present) from nonterminal id 
   * @param token
   * @return cleaned token
   */
  public static String cleanNonTerminal(String token) {
    // cleans NT of any markup, e.g., [X,1] may becomes [X], depending on nonTerminalCleanRegEx
    return token.replaceAll(nonTerminalCleanRegEx, "");
  }

  public static boolean isNonTerminal(final String word) {
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
