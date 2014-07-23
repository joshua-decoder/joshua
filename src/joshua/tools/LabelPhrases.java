package joshua.tools;

import java.io.IOException;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.corpus.syntax.ArraySyntaxTree;
import joshua.util.io.LineReader;

/**
 * Finds labeling for a set of phrases.
 * 
 * @author Juri Ganitkevitch
 */
public class LabelPhrases {

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(LabelPhrases.class.getName());

  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws IOException
   * @throws NumberFormatException
   */
  public static void main(String[] args) throws NumberFormatException, IOException {

    if (args.length < 1 || args[0].equals("-h")) {
      System.err.println("Usage: " + LabelPhrases.class.toString());
      System.err.println("    -p phrase_file     phrase-sentence file to process");
      System.err.println();
      System.exit(-1);
    }

    String phrase_file_name = null;

    for (int i = 0; i < args.length; i++) {
      if ("-p".equals(args[i])) phrase_file_name = args[++i];
    }
    if (phrase_file_name == null) {
      logger.severe("a phrase file is required for operation");
      System.exit(-1);
    }

    LineReader phrase_reader = new LineReader(phrase_file_name);

    while (phrase_reader.ready()) {
      String line = phrase_reader.readLine();

      String[] fields = line.split("\\t");
      if (fields.length != 3 || fields[2].equals("()")) {
        System.err.println("[FAIL] Empty parse in line:\t" + line);
        continue;
      }

      String[] phrase_strings = fields[0].split("\\s");
      int[] phrase_ids = new int[phrase_strings.length];
      for (int i = 0; i < phrase_strings.length; i++)
        phrase_ids[i] = Vocabulary.id(phrase_strings[i]);

      ArraySyntaxTree syntax = new ArraySyntaxTree(fields[2]);
      int[] sentence_ids = syntax.getTerminals();

      int match_start = -1;
      int match_end = -1;
      for (int i = 0; i < sentence_ids.length; i++) {
        if (phrase_ids[0] == sentence_ids[i]) {
          match_start = i;
          int j = 0;
          while (j < phrase_ids.length && phrase_ids[j] == sentence_ids[i + j]) {
            j++;
          }
          if (j == phrase_ids.length) {
            match_end = i + j;
            break;
          }
        }
      }

      int label = syntax.getOneConstituent(match_start, match_end);
      if (label == 0) label = syntax.getOneSingleConcatenation(match_start, match_end);
      if (label == 0) label = syntax.getOneRightSideCCG(match_start, match_end);
      if (label == 0) label = syntax.getOneLeftSideCCG(match_start, match_end);
      if (label == 0) label = syntax.getOneDoubleConcatenation(match_start, match_end);
      if (label == 0) {
        System.err.println("[FAIL] No label found in line:\t" + line);
        continue;
      }

      System.out.println(Vocabulary.word(label) + "\t" + line);
    }
  }
}
