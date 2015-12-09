package joshua.decoder.ff.tm;

import static joshua.decoder.ff.tm.packed.PackedGrammar.VOCABULARY_FILENAME;
import static joshua.util.FormatUtils.cleanNonTerminal;
import static joshua.util.FormatUtils.isNonterminal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.util.io.LineReader;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


public class CreateGlueGrammar {
  
  
  private final Set<String> nonTerminalSymbols = new HashSet<>();
  private static final Logger log = Logger.getLogger(CreateGlueGrammar.class.getName());
  
  @Option(name = "--grammar", aliases = {"-g"}, required = true, usage = "provide grammar to determine list of NonTerminal symbols.")
  private String grammarPath;
  
  @Option(name = "--goal", aliases = {"-goal"}, required = false, usage = "specify custom GOAL symbol. Default: 'GOAL'")
  private String goalSymbol = cleanNonTerminal(new JoshuaConfiguration().goal_symbol);

  /* Rule templates */
  // [GOAL] ||| <s> ||| <s> ||| 0
  private static final String R_START = "[%1$s] ||| <s> ||| <s> ||| 0";
  // [GOAL] ||| [GOAL,1] [X,2] ||| [GOAL,1] [X,2] ||| -1
  private static final String R_TWO = "[%1$s] ||| [%1$s,1] [%2$s,2] ||| [%1$s,1] [%2$s,2] ||| -1";
  // [GOAL] ||| [GOAL,1] </s> ||| [GOAL,1] </s> ||| 0
  private static final String R_END = "[%1$s] ||| [%1$s,1] </s> ||| [%1$s,1] </s> ||| 0";
  // [GOAL] ||| <s> [X,1] </s> ||| <s> [X,1] </s> ||| 0
  private static final String R_TOP = "[%1$s] ||| <s> [%2$s,1] </s> ||| <s> [%2$s,1] </s> ||| 0";
  
  private void run() throws IOException {
    
    File grammar_file = new File(grammarPath);
    if (!grammar_file.exists()) {
      throw new IOException("Grammar file doesn't exist: " + grammarPath);
    }

    // in case of a packedGrammar, we read the serialized vocabulary,
    // collecting all cleaned nonTerminal symbols.
    if (grammar_file.isDirectory()) {
      Vocabulary.read(new File(grammarPath + File.separator + VOCABULARY_FILENAME));
      for (int i = 0; i < Vocabulary.size(); ++i) {
        final String token = Vocabulary.word(i);
        if (isNonterminal(token)) {
          nonTerminalSymbols.add(cleanNonTerminal(token));
        }
      }
    // otherwise we collect cleaned left-hand sides from the rules in the text grammar.
    } else { 
      final LineReader reader = new LineReader(grammarPath);
      while (reader.hasNext()) {
        final String line = reader.next();
        int lhsStart = line.indexOf("[") + 1;
        int lhsEnd = line.indexOf("]");
        if (lhsStart < 1 || lhsEnd < 0) {
          log.info(String.format("malformed rule: %s\n", line));
          continue;
        }
        final String lhs = line.substring(lhsStart, lhsEnd);
        nonTerminalSymbols.add(lhs);
      }
    }
    
    log.info(
        String.format("%d nonTerminal symbols read: %s",
        nonTerminalSymbols.size(),
        nonTerminalSymbols.toString()));

    // write glue rules to stdout
    
    System.out.println(String.format(R_START, goalSymbol));
    
    for (String nt : nonTerminalSymbols)
      System.out.println(String.format(R_TWO, goalSymbol, nt));
    
    System.out.println(String.format(R_END, goalSymbol));
    
    for (String nt : nonTerminalSymbols)
      System.out.println(String.format(R_TOP, goalSymbol, nt));

  }
  
  public static void main(String[] args) throws IOException {
    final CreateGlueGrammar glueCreator = new CreateGlueGrammar();
    final CmdLineParser parser = new CmdLineParser(glueCreator);

    try {
      parser.parseArgument(args);
      glueCreator.run();
    } catch (CmdLineException e) {
      log.info(e.toString());
      parser.printUsage(System.err);
      System.exit(1);
    }
   }
}
