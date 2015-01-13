package joshua.decoder.ff.tm;

/***
 * A class for reading in rules from a Moses phrase table. Most of the conversion work is done
 * in {@link joshua.decoder.ff.tm.format.MosesFormatReader}. This includes prepending every
 * rule with a nonterminal, so that the phrase-based decoder can assume the same hypergraph
 * format as the hierarchical decoder (by pretending to be a strictly left-branching grammar and
 * dispensing with the notion of coverage spans). However, prepending the nonterminals means all
 * the alignments are off by 1. We do not want to fix those when reading in due to the expense,
 * so instead we use this rule which adjust the alignments on the fly.
 * 
 * @author Matt Post
 *
 */
public class MosesPhraseRule extends Rule {

  public MosesPhraseRule(int lhs, int[] french, int[] english, String sparse_features, int arity,
      String alignment) {
    super(lhs, french, english, sparse_features, arity, alignment);
  }

  /**
   * This is the exact same as the parent implementation, but we need to add 1 to each alignment
   * point to account for the nonterminal [X] that was prepended to each rule. 
   */
  @Override
  public byte[] getAlignment() {
    if (alignment == null) {
      String[] tokens = getAlignmentString().split("[-\\s]+");
      alignment = new byte[tokens.length];
      alignment[0] = alignment[1] = 0;
      for (int i = 0; i < tokens.length; i++)
        alignment[i + 2] = (byte) (Short.parseShort(tokens[i]) + 1);
    }
    return alignment;
  }
}
