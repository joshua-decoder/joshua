package joshua.decoder.segment_file;

import joshua.corpus.Vocabulary;
import joshua.corpus.syntax.ArraySyntaxTree;
import joshua.corpus.syntax.SyntaxTree;
import joshua.decoder.JoshuaConfiguration;

public class ParsedSentence extends Sentence {

  private SyntaxTree syntaxTree = null;

  public ParsedSentence(String input, int id,JoshuaConfiguration joshuaConfiguration) {
    super(input, id, joshuaConfiguration);
  }

  public int[] getWordIDs() {
    int[] terminals = syntaxTree().getTerminals();
    int[] annotated = new int[terminals.length + 2];
    System.arraycopy(terminals, 0, annotated, 1, terminals.length);
    annotated[0] = Vocabulary.id(Vocabulary.START_SYM);
    annotated[annotated.length - 1] = Vocabulary.id(Vocabulary.STOP_SYM);
    return annotated;
  }

  public SyntaxTree syntaxTree() {
    if (syntaxTree == null)
      syntaxTree = new ArraySyntaxTree(this.source());
    return syntaxTree;
  }

  public static boolean matches(String input) {
    return input.matches("^\\(+[A-Z]+ .*");
  }

  public String fullSource() {
    return Vocabulary.getWords(this.getWordIDs());
  }
}
