package joshua.decoder.segment_file;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.JoshuaConfiguration;
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

  private static final Logger logger = Logger.getLogger(Sentence.class.getName());

  /* The sentence number. */
  private int id = -1;

  /*
   * The source and target sides of the input sentence. Target sides are present when doing
   * alignment or forced decoding.
   */
  protected String sentence;
  protected String target = null;

  /* Lattice representation of the source sentence. */
  protected Lattice<Integer> sourceLattice = null;

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
   * Returns the length of the sentence. For lattices, the length is the shortest path through the
   * lattice.
   */
  private int length() {
    return this.intLattice().getShortestDistance();
  }

  /*
   * This function uses the supplied grammars to find OOVs in its input and create "detours" around
   * them by splitting the OOVs on internal word boundaries. The idea is to break apart noun
   * compounds in languages like German (such as the word "golfloch" = "golf" (golf) + "loch" (hole)
   * that artificiallly inflate the vocabulary with OOVs.
   */
  public void addOOVDetour(List<GrammarFactory> grammars) {
    
    
  }

  /**
   * Hacky approach: if the input sentence is deemed too long, replace it (and the target, if not
   * null) with an empty string.
   */
  private void adjustForLength() {

    Lattice<Integer> lattice = this.intLattice();
    int size = lattice.size() - 2; // subtract off the start- and end-of-sentence tokens

    if (size > JoshuaConfiguration.maxlen - 2) {
      logger.warning(String.format("* WARNING: sentence %d too long (%d), truncating to length %d",
          id(), size, JoshuaConfiguration.maxlen));

      // Replace the input sentence (and target)
      String[] tokens = source().split("\\s+");
      sentence = tokens[0];
      for (int i = 1; i < JoshuaConfiguration.maxlen; i++)
        sentence += " " + tokens[i];
      sourceLattice = null;
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
    if (this.sourceLattice == null)
      this.sourceLattice = Lattice.createIntLattice(intSentence());
    return this.sourceLattice;
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
