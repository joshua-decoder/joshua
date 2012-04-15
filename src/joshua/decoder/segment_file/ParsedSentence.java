package joshua.decoder.segment_file;

import joshua.corpus.syntax.ArraySyntaxTree;
import joshua.corpus.syntax.SyntaxTree;

public class ParsedSentence extends Sentence {

  private SyntaxTree syntaxTree;

  public ParsedSentence(String input, int id) {
    super(input, id);
    syntaxTree = new ArraySyntaxTree(this.sentence());
  }

  public int[] intSentence() {
    return syntaxTree.getTerminals();
  }

  public SyntaxTree syntaxTree() {
    return syntaxTree;
  }

  public static boolean matches(String input) {
    return input.matches("^\\(+[A-Z]+ .*");
  }
}
