package joshua.decoder.segment_file;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.JoshuaConfiguration;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;
import joshua.util.ChartSpan;
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
  protected String[] references = null; 
  
  /* Lattice representation of the source sentence. */
  protected Lattice<Integer> sourceLattice = null;

  private final List<ConstraintSpan> constraints;

  // Matches the opening and closing <seg> tags, e.g.,
  // <seg id="72">this is a test input sentence</seg>.
  protected static final Pattern SEG_START = Pattern
      .compile("^\\s*<seg\\s+id=\"?(\\d+)\"?[^>]*>\\s*");
  protected static final Pattern SEG_END = Pattern.compile("\\s*</seg\\s*>\\s*$");

  /**
   * Constructor. Receives a string representing the input sentence. This string may be a
   * string-encoded lattice or a plain text string for decoding.
   * 
   * @param inputSentence
   * @param id
   */
  public Sentence(String inputSentence, int id, JoshuaConfiguration joshuaConfiguration) {

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
        String[] pieces = inputSentence.split("\\s?\\|{3}\\s?");
        sentence = pieces[0];
        target = pieces[1];
        if (target.equals(""))
          target = null;
        if (pieces.length > 2) {
          references = new String[pieces.length - 2];
          System.arraycopy(pieces, 2, references, 0, pieces.length - 2);
        }
      } else {
        sentence = inputSentence;
      }
      this.id = id;
    }

    // A maxlen of 0 means no limit. Only trim lattices that are linear chains.
    if (joshuaConfiguration.maxlen != 0 && ! this.intLattice().hasMoreThanOnePath())
      adjustForLength(joshuaConfiguration.maxlen);
  }

  /**
   * Returns the length of the sentence. For lattices, the length is the shortest path through the
   * lattice.
   */
  public int length() {
    return this.intLattice().getShortestDistance();
  }

  /**
   * This function uses the supplied grammars to find OOVs in its input and create "detours" around
   * them by splitting the OOVs on internal word boundaries. The idea is to break apart noun
   * compounds in languages like German (such as the word "golfloch" = "golf" (golf) + "loch" (hole)
   * that artificially inflate the vocabulary with OOVs.
   * 
   * @param grammars a list of grammars to consult to find in- and out-of-vocabulary items
   */
  public void addOOVDetours(List<Grammar> grammars) {
    Lattice<Integer> lattice = this.intLattice();
    
    Node<Integer> node = lattice.getNode(0);
    for (Arc<Integer> arc : node.getOutgoingArcs()) {
      int label = arc.getLabel();
      boolean isOOV = true;
      for (Grammar grammar: grammars) {
        if (grammar.getTrieRoot().match(label) != null) {
          isOOV = false;
          break;
        }
      }

      /* If the word is an OOV, we now parse it at the character-level, with cells in the dynamic programming
       * chart recording whether each span represents a valid decomposition of in-vocabulary sequences of words.
       */
      if (isOOV) {
        String word = Vocabulary.word(label);
        ChartSpan<Boolean> chart = new ChartSpan<Boolean>(word.length(), false);

        for (int width = 1; width <= word.length(); width++) {
          for (int i = 0; i <= word.length() - width; i++) {
            int j = i + width;
            
            // TODO: finish this
            chart.set(i, j, true);
          }
        }
        
      }
      
      // Node<Integer> head = arc.getHead();
    }
  }

  /**
   * If the input sentence is too long (not counting the <s> and </s> tokens), it is truncated to
   * the maximum length, specified with the "maxlen" parameter.
   * 
   * Note that this code assumes the underlying representation is a sentence, and not a lattice. Its
   * behavior is undefined for lattices.
   * @param length 
   */
  private void adjustForLength(int length) {
    int size = this.intLattice().size() - 2; // subtract off the start- and end-of-sentence tokens

    if (size > length) {
      logger.warning(String.format("* WARNING: sentence %d too long (%d), truncating to length %d",
          id(), size, length));

      // Replace the input sentence (and target)
      String[] tokens = source().split("\\s+");
      sentence = tokens[0];
      for (int i = 1; i < length; i++)
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
  
  public String[] references() {
    return references;
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
