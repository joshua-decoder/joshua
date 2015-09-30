package joshua.decoder.ff.tm;

/***
 * A class for reading in rules from a Moses phrase table. Most of the conversion work is done
 * in {@link joshua.decoder.ff.tm.format.PhraseFormatReader}. This includes prepending every
 * rule with a nonterminal, so that the phrase-based decoder can assume the same hypergraph
 * format as the hierarchical decoder (by pretending to be a strictly left-branching grammar and
 * dispensing with the notion of coverage spans). However, prepending the nonterminals means all
 * the alignments are off by 1. We do not want to fix those when reading in due to the expense,
 * so instead we use this rule which adjust the alignments on the fly.
 * 
 * Also, we only convert the Moses dense features on the fly, via this class.
 * 
 * TODO: this class should also be responsible for prepending the nonterminals.
 * 
 * @author Matt Post
 *
 */
public class PhraseRule extends Rule {

  private String mosesFeatureString = null;
  
  public PhraseRule(int lhs, int[] french, int[] english, String sparse_features, int arity,
      String alignment) {
    super(lhs, french, english, null, arity, alignment);
    mosesFeatureString = sparse_features;
  }

  /** 
   * Moses features are probabilities; we need to convert them here by taking the negative log prob.
   * We do this only when the rule is used to amortize.
   */
  @Override
  public String getFeatureString() {
    if (sparseFeatureString == null) {
      StringBuffer values = new StringBuffer();
      for (String value: mosesFeatureString.split(" ")) {
        float f = Float.parseFloat(value);
        values.append(String.format("%f ", f <= 0.0 ? -100 : -Math.log(f)));
      }
      sparseFeatureString = values.toString().trim();
    }
    return sparseFeatureString;
  }
  
  /**
   * This is the exact same as the parent implementation, but we need to add 1 to each alignment
   * point to account for the nonterminal [X] that was prepended to each rule. 
   */
  @Override
  public byte[] getAlignment() {
    if (alignment == null) {
      String[] tokens = getAlignmentString().split("[-\\s]+");
      alignment = new byte[tokens.length + 2];
      alignment[0] = alignment[1] = 0;
      for (int i = 0; i < tokens.length; i++)
        alignment[i + 2] = (byte) (Short.parseShort(tokens[i]) + 1);
    }
    return alignment;
  }
}
