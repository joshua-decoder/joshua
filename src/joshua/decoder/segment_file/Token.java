package joshua.decoder.segment_file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;

/**
 * Stores the identity of a word and its annotations in a sentence
 * @author "Gaurav Kumar"
 *
 */
public class Token {
  // The token without the annotations
  private String token; 
  private int tokenID;
  // The annotation extracted from the raw token 
  private String type;
  private int typeID;

  /**
   * Constructor : Creates a Token object from a raw word
   * Extracts and assigns an annotation when available.
   * The current convention for annotations is $TYPE_(TOKEN)
   * For e.g., $num_(34) or $place_(Baltimore)
   * Annotations can only be alphanumeric
   * The annotation is set to -1 if there is no annotation for this token 
   * 
   * @param rawWord A word with annotation information (possibly)
   *  
   */
  public Token(String rawWord) {
    // Matches a word with an annotation
    // Check guidelines in constructor description
    Pattern annotation = Pattern.compile("\\$(\\S+)_\\(([^)]+)\\)");
    Matcher tag = annotation.matcher(rawWord);
    if (tag.find()) {
      // Annotation match found
      type = tag.group(1);
      token = tag.group(2);
    } else {
      // No match found, which implies that this token does not have an 
      // associated annotation
      token = rawWord;
      type = null;
    }
    // Get the Vocabulary ID for the token and the tyoe
    // The type string is also in the vocabulary since the LM
    // needs an integer version of the type. 
    tokenID = Vocabulary.id(token);
    typeID = type != null ? Vocabulary.id(type) : -1;
  }

  /**
   * Returns the word ID (vocab ID) for this token
   * 
   * @return int A word ID
   */
  public int getWord() {
    return tokenID;
  }

  /**
   * Returns the string associated with this token
   * @return String A word
   */
  public String getWordIdentity() {
    return token;
  }

  /**
   * Returns the annotationID (vocab ID)
   * associated with this token
   * @return int A type ID
   */
  public int getAnnotation() {
    return typeID;
  }

  /**
   * Returns the string version of the annotation
   * associated with this token
   * @return String A type
   */
  public String getTypeIdentity() {
    return type;
  }
}