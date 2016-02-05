package joshua.decoder;

/*
 * This class is used to capture metadata command to Joshua on input and pass them to the
 * decoder.
 */

public class MetaDataException extends Exception {

  private String[] tokens = null;
  
  public MetaDataException(String message) {
    tokens = message.split(" ");
  }

  public String type() {
    return this.tokens[0].substring(1);
  }
  
  public String[] tokens() {
    return this.tokens;
  }
}
