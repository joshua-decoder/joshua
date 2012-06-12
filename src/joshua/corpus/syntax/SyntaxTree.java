package joshua.corpus.syntax;

import java.util.Collection;

public interface SyntaxTree {

  public Collection<Integer> getConstituentLabels(int from, int to);

  public Collection<Integer> getConcatenatedLabels(int from, int to);

  public Collection<Integer> getCcgLabels(int from, int to);

  public int[] getTerminals();

  public int[] getTerminals(int from, int to);
}
