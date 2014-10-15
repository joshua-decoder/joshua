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
    String[] tokens = line.split(HieroFormatReader.fieldDelimiter);
    StringBuffer values = new StringBuffer();
    for (String value: tokens[2].split(" ")) {
      float f = Float.parseFloat(value);
      values.append(String.format(" %f", f <= 0.0 ? -100 : -Math.log(f)));
    }
    String newLine = String.format("[X] ||| [X,1] %s ||| [X,1] %s |||%s", tokens[0], tokens[1], values);

    for (int i = 3; i < tokens.length; i++)
      newLine += " ||| " + tokens[i];
    
//    System.err.println(String.format("parseLine(%s) --> %s", line, newLine));
    return super.parseLine(newLine);
  }
}
