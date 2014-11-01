package joshua.decoder.segment_file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
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
    if (joshuaConfiguration.maxlen != 0 && !this.intLattice().hasMoreThanOnePath())
      adjustForLength(joshuaConfiguration.maxlen);
  }

  /**
   * Returns the length of the sentence. For lattices, the length is the shortest path through the
   * lattice. The length includes the <s> and </s> sentence markers. 
   * 
   * @return number of input tokens + 2 (for start and end of sentence markers)
   */
  public int length() {
    return this.intLattice().getShortestDistance();
  }

  /**
   * This function computes the intersection of \sigma^+ (where \sigma is the terminal vocabulary)
   * with all character-level segmentations of each OOV in the input sentence.
   * 
   * The idea is to break apart noun compounds in languages like German (such as the word "golfloch"
   * = "golf" (golf) + "loch" (hole)), allowing them to be translated.
   * 
   * @param grammars a list of grammars to consult to find in- and out-of-vocabulary items
   */
  public void segmentOOVs(Grammar[] grammars) {
    Lattice<Integer> oldLattice = this.intLattice();

    /* Build a list of terminals across all grammars */
    HashSet<Integer> vocabulary = new HashSet<Integer>();
    for (Grammar grammar : grammars) {
      Iterator<Integer> iterator = grammar.getTrieRoot().getTerminalExtensionIterator();
      while (iterator.hasNext())
        vocabulary.add(iterator.next());
    }

    List<Node<Integer>> oldNodes = oldLattice.getNodes();

    /* Find all the subwords that appear in the vocabulary, and create the lattice */
    for (int nodeid = oldNodes.size() - 3; nodeid >= 1; nodeid -= 1) {
      if (oldNodes.get(nodeid).getOutgoingArcs().size() == 1) {
        Arc<Integer> arc = oldNodes.get(nodeid).getOutgoingArcs().get(0);
        String word = Vocabulary.word(arc.getLabel());
        if (!vocabulary.contains(arc.getLabel())) {
          // System.err.println(String.format("REPL: '%s'", word));
          List<Arc<Integer>> savedArcs = oldNodes.get(nodeid).getOutgoingArcs();

          char[] chars = word.toCharArray();
          ChartSpan<Boolean> wordChart = new ChartSpan<Boolean>(chars.length + 1, false);
          ArrayList<Node<Integer>> nodes = new ArrayList<Node<Integer>>(chars.length + 1);
          nodes.add(oldNodes.get(nodeid));
          for (int i = 1; i < chars.length; i++)
            nodes.add(new Node<Integer>(i));
          nodes.add(oldNodes.get(nodeid + 1));
          for (int width = 1; width <= chars.length; width++) {
            for (int i = 0; i <= chars.length - width; i++) {
              int j = i + width;
              if (width != chars.length) {
                Integer id = Vocabulary.id(word.substring(i, j));
                if (vocabulary.contains(id)) {
                  nodes.get(i).addArc(nodes.get(j), 0.0f, id);
                  wordChart.set(i, j, true);
//                  System.err.println(String.format("  FOUND '%s' at (%d,%d)", word.substring(i, j),
//                      i, j));
                }
              }

              for (int k = i + 1; k < j; k++) {
                if (wordChart.get(i, k) && wordChart.get(k, j)) {
                  wordChart.set(i, j, true);
//                  System.err.println(String.format("    PATH FROM %d-%d-%d", i, k, j));
                }
              }
            }
          }

          /* If there's a path from beginning to end */
          if (wordChart.get(0, chars.length)) {
//            System.err.println(String.format("  THERE IS A PATH"));
//
            // Remove nodes not part of a complete path
            HashSet<Node<Integer>> deletedNodes = new HashSet<Node<Integer>>();
            for (int k = 1; k < nodes.size() - 1; k++)
              if (!(wordChart.get(0, k) && wordChart.get(k, chars.length)))
                nodes.set(k, null);

            int delIndex = 1;
            while (delIndex < nodes.size())
              if (nodes.get(delIndex) == null) {
                deletedNodes.add(nodes.get(delIndex));
                nodes.remove(delIndex);
              } else
                delIndex++;

//            System.err.println("  REMAINING NODES:");
            for (Node<Integer> node : nodes) {
//              System.err.println("    NODE: " + node.id());
              int arcno = 0;
              while (arcno != node.getOutgoingArcs().size()) {
                Arc<Integer> delArc = node.getOutgoingArcs().get(arcno);
                if (deletedNodes.contains(delArc.getHead()))
                  node.getOutgoingArcs().remove(arcno);
                else {
                  arcno++;
//                  System.err.println("           ARC: " + Vocabulary.word(delArc.getLabel()));
                }
              }
            }

            // Insert into the main lattice
            this.intLattice().insert(nodeid, nodeid + 1, nodes);
          } else {
//            System.err.println(String.format("  NO PATH from %d-%d", 0, chars.length));

            nodes.get(0).setOutgoingArcs(savedArcs);
          }
        }
      }
    }
  }

  /**
   * If the input sentence is too long (not counting the <s> and </s> tokens), it is truncated to
   * the maximum length, specified with the "maxlen" parameter.
   * 
   * Note that this code assumes the underlying representation is a sentence, and not a lattice. Its
   * behavior is undefined for lattices.
   * 
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
  
  public String fullTarget() {
    return String.format("<s> %s </s>", target());
  }
  
  public String source(int i, int j) {
    StringTokenizer st = new StringTokenizer(annotatedSource());
    int index = 0;
    String substring = "";
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (index >= j)
        break;
      if (index >= i)
        substring += token + " ";
      index++;
    }
    return substring.trim();
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

  public boolean hasPath(int begin, int end) {
    return intLattice().distance(begin, end) != -1;
  }
}
