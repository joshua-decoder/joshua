package joshua.decoder;

/*
 * This class is used to capture metadata command to Joshua on input and pass them to the
 * decoder.
 */

public class MetaDataException extends Exception {
  private String type = null;
  private String tokenString = null;
  
  public MetaDataException(String message) {
    int firstSpace = message.indexOf(' ');
    if (firstSpace != -1) {
      this.type = message.substring(1, firstSpace);
      this.tokenString = message.substring(firstSpace + 1);
    } else if (message.length() > 0) {
      this.type = message.substring(1);
      this.tokenString = "";
    }
  }

  public String type() {
    return this.type;
  }
  
  public String tokenString() {
    return this.tokenString;
  }
  
  public String[] tokens(String regex) {
    return this.tokenString.split(regex);
  }
  
  public String[] tokens() {
    return this.tokens("\\s+");
  }
}
