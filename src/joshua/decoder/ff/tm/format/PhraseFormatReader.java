package joshua.decoder.ff.tm.format;

import joshua.decoder.ff.tm.BilingualRule;

public class PhraseFormatReader extends HieroFormatReader {

  public PhraseFormatReader(String grammarFile) {
    super(grammarFile);
  }
  
  /**
   * This munges a Moses-style phrase table into a grammar.
   * 
   *    mots francaises ||| French words ||| 1 2 3
   *    
   * becomes
   * 
   *    [X] ||| [X,1] mots francaises ||| [X,1] French words ||| 1 2 3
   * 
   */
  @Override
  public BilingualRule parseLine(String line) {
    String newLine = "[X] ||| [X,1] " + line.replaceFirst("|||", "||| [X,1]");
    return super.parseLine(newLine);
  }
}
