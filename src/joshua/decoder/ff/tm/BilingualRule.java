package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Arrays;

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
   * their default names ("tm_OWNER_INDEX") until later, when we know the owner of the rule. This is
   * not known until the rule is actually added to a grammar in Grammar::addRule().
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

  /**
   * The nonterminals on the English side are pointers to the source side nonterminals (-1 and -2),
   * rather than being directly encoded. These number indicate the correspondence between the
   * nonterminals on each side, introducing a level of indirection however when we want to resolve
   * them. So to get the ID, we need to look up the corresponding source side ID.
   * 
   * @return The string of English words
   */
  public String getEnglishWords() {
    ArrayList<String> foreignNTs = new ArrayList<String>();
    for (int i = 0; i < this.getFrench().length; i++)
      if (this.getFrench()[i] < 0) foreignNTs.add(Vocabulary.word(this.getFrench()[i]));

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.getEnglish().length; i++) {
      if (this.getEnglish()[i] >= 0)
        sb.append(Vocabulary.word(this.getEnglish()[i]) + " ");
      else
        sb.append(foreignNTs.get(Math.abs(this.getEnglish()[i]) - 1) + "," + Math.abs(this.getEnglish()[i]) + " ");
    }

    return sb.toString().trim();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.getLHS()));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(this.getFrench()));
    sb.append(" ||| ");
    sb.append(getEnglishWords());
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
