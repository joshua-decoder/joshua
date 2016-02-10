package joshua.decoder.segment_file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.corpus.Vocabulary;
import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;	
import joshua.decoder.ff.tm.Grammar;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;
import joshua.util.ChartSpan;
import joshua.util.Regex;

/**
 * This class represents lattice input. The lattice is contained on a single line and is represented
 * in PLF (Python Lattice Format), e.g.,
 * 
 * ((('ein',0.1,1),('dieses',0.2,1),('haus',0.4,2),),(('haus',0.8,1),),)
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */

public class Sentence {

  /* The sentence number. */
  public int id = -1;

  /*
   * The source and target sides of the input sentence. Target sides are present when doing
   * alignment or forced decoding.
   */
  protected String source = null;
  protected String target = null;
  protected String[] references = null;

  /* Lattice representation of the source sentence. */
  protected Lattice<Token> sourceLattice = null;

  /* List of constraints */
  private final List<ConstraintSpan> constraints;
  
  private JoshuaConfiguration config = null;

  /**
   * Constructor. Receives a string representing the input sentence. This string may be a
   * string-encoded lattice or a plain text string for decoding.
   * 
   * @param inputString
   * @param id
   */
  public Sentence(String inputString, int id, JoshuaConfiguration joshuaConfiguration) {
  
    inputString = Regex.spaces.replaceAll(inputString, " ").trim();
    
    config = joshuaConfiguration;
    
    this.constraints = new LinkedList<ConstraintSpan>();
  
    // Check if the sentence has SGML markings denoting the
    // sentence ID; if so, override the id passed in to the
    // constructor
    Matcher start = SEG_START.matcher(inputString);
    if (start.find()) {
      source = SEG_END.matcher(start.replaceFirst("")).replaceFirst("");
      String idstr = start.group(1);
      this.id = Integer.parseInt(idstr);
    } else {
      if (inputString.indexOf(" ||| ") != -1) {
        String[] pieces = inputString.split("\\s?\\|{3}\\s?");
        source = pieces[0];
        target = pieces[1];
        if (target.equals(""))
          target = null;
        if (pieces.length > 2) {
          references = new String[pieces.length - 2];
          System.arraycopy(pieces, 2, references, 0, pieces.length - 2);
        }
      } else {
        source = inputString;
      }
      this.id = id;
    }
    
    // Mask strings that cause problems for the decoder
    source = source.replaceAll("\\[",  "-lsb-")
        .replaceAll("\\]",  "-rsb-")
        .replaceAll("\\|",  "-pipe-");
  
    // Only trim strings
    if (joshuaConfiguration.lattice_decoding && ! source.startsWith("((("))
      adjustForLength(joshuaConfiguration.maxlen);
  }
  
  /**
   * Indicates whether the underlying lattice is a linear chain, i.e., a sentence.
   * 
   * @return true if this is a linear chain, false otherwise
   */
  public boolean isLinearChain() {
    return ! this.getLattice().hasMoreThanOnePath();
  }

  // Matches the opening and closing <seg> tags, e.g.,
  // <seg id="72">this is a test input sentence</seg>.
  protected static final Pattern SEG_START = Pattern
      .compile("^\\s*<seg\\s+id=\"?(\\d+)\"?[^>]*>\\s*");
  protected static final Pattern SEG_END = Pattern.compile("\\s*</seg\\s*>\\s*$");

  /**
   * Returns the length of the sentence. For lattices, the length is the shortest path through the
   * lattice. The length includes the <s> and </s> sentence markers. 
   * 
   * @return number of input tokens + 2 (for start and end of sentence markers)
   */
  public int length() {
    return this.getLattice().getShortestDistance();
  }

  /**
   * Returns the annotations for a specific word (specified by an index) in the 
   * sentence
   * @param index The location of the word in the sentence
   * @return The annotations associated with this word
   */
  public int getAnnotation(int index) {
    return getTokens().get(index).getAnnotation();
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
    Lattice<Token> oldLattice = this.getLattice();

    /* Build a list of terminals across all grammars */
    HashSet<Integer> vocabulary = new HashSet<Integer>();
    for (Grammar grammar : grammars) {
      Iterator<Integer> iterator = grammar.getTrieRoot().getTerminalExtensionIterator();
      while (iterator.hasNext())
        vocabulary.add(iterator.next());
    }

    List<Node<Token>> oldNodes = oldLattice.getNodes();

    /* Find all the subwords that appear in the vocabulary, and create the lattice */
    for (int nodeid = oldNodes.size() - 3; nodeid >= 1; nodeid -= 1) {
      if (oldNodes.get(nodeid).getOutgoingArcs().size() == 1) {
        Arc<Token> arc = oldNodes.get(nodeid).getOutgoingArcs().get(0);
        String word = Vocabulary.word(arc.getLabel().getWord());
        if (!vocabulary.contains(arc.getLabel())) {
          // System.err.println(String.format("REPL: '%s'", word));
          List<Arc<Token>> savedArcs = oldNodes.get(nodeid).getOutgoingArcs();

          char[] chars = word.toCharArray();
          ChartSpan<Boolean> wordChart = new ChartSpan<Boolean>(chars.length + 1, false);
          ArrayList<Node<Token>> nodes = new ArrayList<Node<Token>>(chars.length + 1);
          nodes.add(oldNodes.get(nodeid));
          for (int i = 1; i < chars.length; i++)
            nodes.add(new Node<Token>(i));
          nodes.add(oldNodes.get(nodeid + 1));
          for (int width = 1; width <= chars.length; width++) {
            for (int i = 0; i <= chars.length - width; i++) {
              int j = i + width;
              if (width != chars.length) {
                Token token = new Token(word.substring(i, j));
                if (vocabulary.contains(id)) {
                  nodes.get(i).addArc(nodes.get(j), 0.0f, token);
                  wordChart.set(i, j, true);
                  //                    System.err.println(String.format("  FOUND '%s' at (%d,%d)", word.substring(i, j),
                  //                        i, j));
                }
              }

              for (int k = i + 1; k < j; k++) {
                if (wordChart.get(i, k) && wordChart.get(k, j)) {
                  wordChart.set(i, j, true);
                  //                    System.err.println(String.format("    PATH FROM %d-%d-%d", i, k, j));
                }
              }
            }
          }

          /* If there's a path from beginning to end */
          if (wordChart.get(0, chars.length)) {
            // Remove nodes not part of a complete path
            HashSet<Node<Token>> deletedNodes = new HashSet<Node<Token>>();
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

            for (Node<Token> node : nodes) {
              int arcno = 0;
              while (arcno != node.getOutgoingArcs().size()) {
                Arc<Token> delArc = node.getOutgoingArcs().get(arcno);
                if (deletedNodes.contains(delArc.getHead()))
                  node.getOutgoingArcs().remove(arcno);
                else {
                  arcno++;
                  //                    System.err.println("           ARC: " + Vocabulary.word(delArc.getLabel()));
                }
              }
            }

            // Insert into the main lattice
            this.getLattice().insert(nodeid, nodeid + 1, nodes);
          } else {
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
  protected void adjustForLength(int length) {
    int size = this.getLattice().size() - 2; // subtract off the start- and end-of-sentence tokens

    if (size > length) {
      Decoder.LOG(1, String.format("* WARNING: sentence %d too long (%d), truncating to length %d",
          id(), size, length));

      // Replace the input sentence (and target) -- use the raw string, not source()
      String[] tokens = source.split("\\s+");
      source = tokens[0];
      for (int i = 1; i < length; i++)
        source += " " + tokens[i];
      sourceLattice = null;
      if (target != null) {
        target = "";
      }
    }
  }

  public boolean isEmpty() {
    return source.matches("^\\s*$");
  }

  public int id() {
    return id;
  }

  /**
   * Returns the raw source-side input string.
   */
  public String rawSource() {
    return source;
  }
  
  /**
   * Returns the source-side string with annotations --- if any --- stripped off.
   * 
   * @return
   */
  public String source() {
    String str = "";
    int[] ids = getWordIDs();
    for (int i = 1; i < ids.length - 1; i++)
      str += Vocabulary.word(ids[i]) + " ";
    return str.trim();
  }

  /**
   * Returns a sentence with the start and stop symbols added to the 
   * beginning and the end of the sentence respectively
   * 
   * @return String The input sentence with start and stop symbols
   */
  public String fullSource() {
    return String.format("%s %s %s", Vocabulary.START_SYM , source(), Vocabulary.STOP_SYM); 
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
    StringTokenizer st = new StringTokenizer(fullSource());
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

  /**
   * Returns the sequence of tokens comprising the sentence. This assumes you've done the checking
   * to makes sure the input string (the source side) isn't a PLF waiting to be parsed.
   * 
   * @return
   */
  public List<Token> getTokens() {
    assert isLinearChain();
    List<Token> tokens = new ArrayList<Token>();
    for (Node<Token> node: getLattice().getNodes())
      if (node != null && node.getOutgoingArcs().size() > 0) 
        tokens.add(node.getOutgoingArcs().get(0).getLabel());
    return tokens;
  }
  
  /**
   * Returns the sequence of word IDs comprising the input sentence. Assumes this is not a general
   * lattice, but a linear chain.
   */
  public int[] getWordIDs() {
    List<Token> tokens = getTokens();
    int[] ids = new int[tokens.size()];
    for (int i = 0; i < tokens.size(); i++)
      ids[i] = tokens.get(i).getWord();
    return ids;
  }
  
  /**
   * Returns the sequence of word ids comprising the sentence. Assumes this is a sentence and
   * not a lattice.
   *  
   * @return
   */
  public Lattice<String> stringLattice() {
    assert isLinearChain();
    return Lattice.createStringLatticeFromString(source());
  }

  public List<ConstraintSpan> constraints() {
    return constraints;
  }

  public Lattice<Token> getLattice() {
    if (this.sourceLattice == null) {
      if (config.lattice_decoding && rawSource().startsWith("(((")) {
        if (config.search_algorithm.equals("stack")) {
          System.err.println("* FATAL: lattice decoding currently not supported for stack-based search algorithm.");
          System.exit(12);
        }
        this.sourceLattice = Lattice.createTokenLatticeFromPLF(rawSource());
      } else
        this.sourceLattice = Lattice.createTokenLatticeFromString(String.format("%s %s %s", Vocabulary.START_SYM,
            rawSource(), Vocabulary.STOP_SYM));
    }
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
    return getLattice().distance(begin, end) != -1;
  }

  public Node<Token> getNode(int i) {
    return getLattice().getNode(i);
  }
}
