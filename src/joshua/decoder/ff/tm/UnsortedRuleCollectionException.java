package joshua.decoder.ff.tm;

/**
 * Unchecked runtime exception thrown to indicate that a collection of rules has not been properly
 * sorted according to the feature functions in effect.
 * 
 * @author Lane Schwartz
 */
public class UnsortedRuleCollectionException extends RuntimeException {

  private static final long serialVersionUID = -4819014771607378835L;

  /**
   * Constructs an <code>UnsortedRuleCollectionException</code> with the specified detail message.
   * 
   * @param message the detail message
   */
  public UnsortedRuleCollectionException(String message) {
    super(message);
  }

}
