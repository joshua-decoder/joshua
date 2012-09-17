package joshua.decoder.ff.tm;

import java.util.Arrays;
import java.util.Map;

import joshua.corpus.Vocabulary;

/**
 * Normally, the feature score in the rule should be *cost* (i.e., -LogP), so that the feature
 * weight should be positive
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class BilingualRule extends MonolingualRule {

  private int[] english;

  // ===============================================================
  // Constructors
  // ===============================================================

  /**
   * Constructs a new rule using the provided parameters. The owner and rule id for this rule are
   * undefined. Note that some of the sparse features may be unlabeled, but they cannot be mapped to
   * their default names ("PhraseModel_OWNER_INDEX") until later, when we know the owner of the
   * rule. This is not known until the rule is actually added to a grammar in Grammar::addRule().
   * 
   * @param lhs Left-hand side of the rule.
   * @param sourceRhs Source language right-hand side of the rule.
   * @param targetRhs Target language right-hand side of the rule.
   * @param sparseFeatures Feature value scores for the rule.
   * @param arity Number of nonterminals in the source language right-hand side.
   * @param owner
   */
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity,
      int owner) {
    super(lhs, sourceRhs, sparseFeatures, arity, owner);
    this.english = targetRhs;
  }

  // Sparse feature version
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity) {
    super(lhs, sourceRhs, sparseFeatures, arity);
    this.english = targetRhs;
  }


  // ===============================================================
  // Attributes
  // ===============================================================

  public final void setEnglish(int[] eng) {
    this.english = eng;
  }

  public final int[] getEnglish() {
    return this.english;
  }

  // ===============================================================
  // Serialization Methods
  // ===============================================================
  // TODO: remove these methods

  // Caching this method significantly improves performance
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(this.getFrench()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(this.getEnglish()));
    sb.append(" |||");
    sb.append(" " + getFeatureVector());
    sb.append(String.format(" ||| %.3f", getEstimatedCost()));
    return sb.toString();
  }

  /**
   * Two BilingualRules are equal of they have the same LHS, the same source RHS and the same target
   * RHS.
   * 
   * @param o the object to check for equality
   * @return true if o is the same BilingualRule as this rule, false otherwise
   */
  public boolean equals(Object o) {
    if (!(o instanceof BilingualRule)) {
      return false;
    }
    BilingualRule other = (BilingualRule) o;
    if (getLHS() != other.getLHS()) {
      return false;
    }
    if (!Arrays.equals(getFrench(), other.getFrench())) {
      return false;
    }
    if (!Arrays.equals(english, other.getEnglish())) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    // I just made this up. If two rules are equal they'll have the
    // same hashcode. Maybe someone else can do a better job though?
    int frHash = Arrays.hashCode(getFrench());
    int enHash = Arrays.hashCode(english);
    return frHash ^ enHash ^ getLHS();
  }

}
