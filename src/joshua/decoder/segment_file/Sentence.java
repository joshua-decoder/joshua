package joshua.decoder.segment_file;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;
import joshua.lattice.Lattice;
import joshua.util.Regex;

/**
 * This class represents a basic input sentence. A sentence is a sequence of UTF-8 characters
 * denoting a string of source language words. The sequence can optionally be wrapped in <seg
 * id="N">...</seg> tags, which are then used to set the sentence number (a 0-indexed ID).
 *
 * @author Matt Post <post@cs.jhu.edu>
 */

public class Sentence {

  /**
   * The maximum number of tokens in a Sentence before the sentence gets replaced by an empty
   * sentence.
   * <UL>
   * <LI>TODO: Move this setting to JoshuaConfiguration and provide a configurable parameter to the
   * user.</LI>
   * <LI>TODO: Provide the option to truncate to this many tokens instead of making the whole input
   * empty.</LI>
   * </UL>
   */
  public static final int MAX_SENTENCE_NODES = 200;

  private static final Logger logger = Logger.getLogger(Sentence.class.getName());

  /*
   * The distinction between sequenceId and id is important. The former is the identifier assigned
   * by the input handler; these are guaranteed to be sequential with no missing numbers. However,
   * sentences themselves can claim to be whatever number they want (for example, if wrapped in <seg
   * id=N>...</seg> tags). It's important to respect what the sentence claims to be for tuning
   * procedures, but the sequence id is also necessary for ensuring that the output translations are
   * assembled in the order they were found in the input file.
   *
   * In most cases, these numbers should be the same.
   */

  private int id = -1;
  protected String sentence;
  protected String target = null;

  private final List<ConstraintSpan> constraints;

  // Matches the opening and closing <seg> tags, e.g.,
  // <seg id="72">this is a test input sentence</seg>.
  protected static final Pattern SEG_START = Pattern
      .compile("^\\s*<seg\\s+id=\"?(\\d+)\"?[^>]*>\\s*");
  protected static final Pattern SEG_END = Pattern.compile("\\s*</seg\\s*>\\s*$");

  public Sentence(String inputSentence, int id) {

    inputSentence = Regex.spaces.replaceAll(inputSentence, " ").trim();

    constraints = new LinkedList<ConstraintSpan>();

    // Check if the sentence has SGML markings denoting the
    // sentence ID; if so, override the id passed in to the
    // constructor
    Matcher start = SEG_START.matcher(inputSentence);
    if (start.find()) {
      sentence = SEG_END.matcher(start.replaceFirst("")).replaceFirst("");
      String idstr = start.group(1);
      this.id = Integer.parseInt(idstr);
    } else {
      if (inputSentence.indexOf(" ||| ") != -1) {
        String[] pieces = inputSentence.split("\\s\\|{3}\\s", 2);
        sentence = pieces[0];
        target = pieces[1];
      } else {
        sentence = inputSentence;
      }
      this.id = id;
    }
    adjustForLength();
  }

  /**
   * Returns the length of the sentence.  For lattices, the length is the shortest path through the
   * lattice.
   */
  private int length() {
    return this.intLattice().getShortestDistance();
  }

  /**
   * Hacky approach: if the input sentence is deemed too long, replace it (and the target, if not
   * null) with an empty string.
   */
  private void adjustForLength() {

    Lattice<Integer> lattice = this.intLattice();
    int size = lattice.size();

    if (size > MAX_SENTENCE_NODES) {
      logger.warning(String.format("* WARNING: sentence %d too long (%d), truncating to 0 length", id(), size));
      
      // Replace the input sentence (and target)
      sentence = "";
      if (target != null) {
        target = "";
      }
    }
  }

  public boolean isEmpty() {
    return sentence.matches("^\\s*$");
  }

  public int id() {
    return id;
  }

  public String source() {
    return sentence;
  }

  public String annotatedSource() {
    return Vocabulary.START_SYM + " " + sentence + " " + Vocabulary.STOP_SYM;
  }

  /**
   * If a target side was supplied with the sentence, this will be non-null. This is used when doing
   * synchronous parsing or constrained decoding. The input format is:
   *
   * Bill quiere ir a casa ||| Bill wants to go home
   *
   * If the parameter parse=true is set, parsing will be triggered, otherwise constrained decoding.
   *
   * @return
   */
  public String target() {
    return target;
  }

  public int[] intSentence() {
    return Vocabulary.addAll(annotatedSource());
  }

  public List<ConstraintSpan> constraints() {
    return constraints;
  }

  public Lattice<Integer> intLattice() {
    return Lattice.createIntLattice(intSentence());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(source());
    if (target() != null) {
      sb.append(" ||| " + target());
    }
    return sb.toString();
  }
}
