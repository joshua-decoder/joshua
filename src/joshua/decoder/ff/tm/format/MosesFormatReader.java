package joshua.decoder.ff.tm.format;

import joshua.decoder.ff.tm.BilingualRule;
import joshua.util.io.LineReader;

/***
 * This class reads in the Moses phrase table format, with support for the source and target side,
 * list of features, and word alignments. It works by simply casting the phrase-based rules to
 * left-branching hierarchical rules and passing them on to its parent class, {@HieroFormatReader}.
 * 
 * There is also a tool to convert the grammars directly, so that they can be suitably packed. Usage:
 * 
 * <pre>
 *     cat PHRASE_TABLE | java -cp $JOSHUA/class joshua.decoder.ff.tm.format.PhraseFormatReader > grammar
 * </pre>
 * 
 * @author Matt Post <post@cs.jhu.edu>
 *
 */

public class MosesFormatReader extends HieroFormatReader {

  public MosesFormatReader(String grammarFile) {
    super(grammarFile);
  }
  
  public MosesFormatReader() {
    super();
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
  
  /**
   * Converts a Moses phrase table to a Joshua grammar. 
   * 
   * @param args
   */
  public static void main(String[] args) {
    MosesFormatReader reader = new MosesFormatReader();
    for (String line: new LineReader(System.in)) {
      BilingualRule rule = reader.parseLine(line);
      System.out.println(rule.textFormat());
    }    
  }
}
