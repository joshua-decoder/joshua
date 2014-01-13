package joshua.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class TestSetFilter {
  private Filter filter = null;

  // for caching of accepted rules
  private String lastSourceSide;
  private boolean acceptedLastSourceSide;

  public int cached = 0;
  public int RULE_LENGTH = 12;
  public boolean verbose = false;
  public boolean parallel = false;

  private static final String DELIMITER = "|||";
  private static final String DELIMITER_REGEX = " \\|\\|\\| ";
  public static final String DELIM = String.format(" %s ", DELIMITER);
  public static final Pattern P_DELIM = Pattern.compile(DELIMITER_REGEX);
  private final String NT_REGEX = "\\[[^\\]]+?\\]";

  public TestSetFilter() {
    acceptedLastSourceSide = false;
    lastSourceSide = null;
  }
  
  public String getFilterName() {
    if (filter != null)
      if (filter instanceof FastFilter)
        return "fast";
      else if (filter instanceof LooseFilter)
        return "loose";
      else
        return "exact";
    return "null";
  }

  public void setVerbose(boolean value) {
    verbose = value;
  }

  public void setParallel(boolean value) {
    parallel = value;
  }

  public void setFilter(String type) {
    if (type.equals("fast"))
      filter = new FastFilter();
    else if (type.equals("exact"))
      filter = new ExactFilter();
    else if (type.equals("loose"))
      filter = new LooseFilter();
    else
      throw new RuntimeException(String.format("Invalid filter type '%s'", type));
  }

  public void setRuleLength(int value) {
    RULE_LENGTH = value;
  }

  private void loadTestSentences(String filename) {
    int count = 0;

    try {
      Scanner scanner = new Scanner(new File(filename), "UTF-8");
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        filter.addSentence(line);
        count++;
      }
    } catch (FileNotFoundException e) {
      System.err.printf("Could not open %s\n", e.getMessage());
    }

    if (verbose)
      System.err.println(String.format("Added %d sentences.\n", count));
  }

  /**
   * Top-level filter, responsible for calling the fast or exact version.
   */
  public boolean inTestSet(String sourceSide) {
    if (!sourceSide.equals(lastSourceSide)) {
      lastSourceSide = sourceSide;
      acceptedLastSourceSide = filter.permits(sourceSide);
    } else {
      cached++;
    }

    return acceptedLastSourceSide;
  }
    
  /**
   * Determines whether a rule is an abstract rule. An abstract rule is one that has no terminals on
   * its source side.
   * 
   * If the rule is abstract, the rule's arity is returned. Otherwise, 0 is returned.
   */
  private boolean isAbstract(String source) {
    int nonterminalCount = 0;
    for (String t : source.split("\\s+")) {
      if (!t.matches(NT_REGEX))
        return false;
      nonterminalCount++;
    }
    return nonterminalCount != 0;
  }

  private interface Filter {
    public void addSentence(String sentence);
    public boolean permits(String source);
  }

  private class FastFilter implements Filter {
    private Set<String> ngrams = null;

    public FastFilter() {
      ngrams = new HashSet<String>();
    }
    
    @Override
    public boolean permits(String source) {
      for (String chunk : source.split(NT_REGEX)) {
        chunk = chunk.trim();
        /* Important: you need to make sure the string isn't empty. */
        if (!chunk.equals("") && !ngrams.contains(chunk))
          return false;
      }
      return true;
    }

    @Override
    public void addSentence(String sentence) {
      String[] tokens = sentence.trim().split("\\s+");
      int maxOrder = RULE_LENGTH < tokens.length ? RULE_LENGTH : tokens.length;
      for (int order = 1; order <= maxOrder; order++) {
        for (int start = 0; start < tokens.length - order + 1; start++)
          ngrams.add(createNGram(tokens, start, order));
      }
    }

    private String createNGram(String[] tokens, int start, int order) {
      if (order < 1 || start + order > tokens.length) {
        return "";
      }
      String result = tokens[start];
      for (int i = 1; i < order; i++)
        result += " " + tokens[start + i];
      return result;
    }
  }

  private class LooseFilter implements Filter {
    List<String> testSentences = null;

    public LooseFilter() {
      testSentences = new ArrayList<String>();
    }
    
    @Override
    public void addSentence(String source) {
      testSentences.add(source);
    }

    @Override
    public boolean permits(String source) {
      Pattern pattern = getPattern(source);
      for (String testSentence : testSentences) {
        if (pattern.matcher(testSentence).find()) {
          return true;
        }
      }
      return isAbstract(source);
    }

    protected Pattern getPattern(String source) {
      String pattern = source;
      pattern = pattern.replaceAll(String.format("\\s*%s\\s*", NT_REGEX), ".+");
      pattern = pattern.replaceAll("\\s+", ".*");
//      System.err.println(String.format("PATTERN(%s) = %s", source, pattern));
      return Pattern.compile(pattern);
    }
  }

  /**
   * This class is the same as LooseFilter except with a tighter regex for matching rules.
   */
  private class ExactFilter implements Filter {
    private FastFilter fastFilter = null;
    private Map<String, Set<Integer>> sentencesByWord;
    List<String> testSentences = null;
    
    public ExactFilter() {
      fastFilter = new FastFilter();
      sentencesByWord = new HashMap<String, Set<Integer>>();
      testSentences = new ArrayList<String>();
    }
    
    @Override
    public void addSentence(String source) {
      fastFilter.addSentence(source);
      addSentenceToWordHash(source, testSentences.size());
      testSentences.add(source);
    }
    
    @Override
    public boolean permits(String source) {
      if (fastFilter.permits(source)) {
        Pattern pattern = getPattern(source);
        for (int i : getSentencesForRule(source)) {
          if (pattern.matcher(testSentences.get(i)).find()) {
            return true;
          }
        }
        return isAbstract(source);
      }
      return false;
    }
    
    protected Pattern getPattern(String source) {
      String pattern = Pattern.quote(source);
      pattern = pattern.replaceAll(NT_REGEX, "\\\\E.+\\\\Q");
      pattern = pattern.replaceAll("\\\\Q\\\\E", "");
      pattern = "(?:^|\\s)" + pattern + "(?:$|\\s)";
      return Pattern.compile(pattern);
    }
  
    /*
     * Map words to all the sentences they appear in.
     */
    private void addSentenceToWordHash(String sentence, int index) {
      String[] tokens = sentence.split("\\s+");
      for (String t : tokens) {
        if (! sentencesByWord.containsKey(t))
          sentencesByWord.put(t, new HashSet<Integer>());
        sentencesByWord.get(t).add(index);
      }
    }
    
    private Set<Integer> getSentencesForRule(String source) {
      Set<Integer> sentences = null;
      for (String t : source.split("\\s+")) {
        if (!t.matches(NT_REGEX)) {
          if (sentencesByWord.containsKey(t)) {
            if (sentences == null)
              sentences = new HashSet<Integer>(sentencesByWord.get(t));
            else
              sentences.retainAll(sentencesByWord.get(t));
          }
        }
      }
      
      return sentences;
    }
  }

  public static void main(String[] argv) {
    // do some setup
    if (argv.length < 1) {
      System.err.println("usage: TestSetFilter [-v|-p|-f|-n N] <test set1> [test set2 ...]");
      System.err.println("    -v    verbose output");
      System.err.println("    -p    parallel compatibility");
      System.err.println("    -f    fast mode");
      System.err.println("    -n    max n-gram to compare to (default 12)");
      return;
    }

    TestSetFilter filter = new TestSetFilter();

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-v")) {
        filter.setVerbose(true);
        continue;
      } else if (argv[i].equals("-p")) {
        filter.setParallel(true);
        continue;
      } else if (argv[i].equals("-f")) {
        filter.setFilter("fast");
        continue;
      } else if (argv[i].equals("-e")) {
        filter.setFilter("exact");
        continue;
      } else if (argv[i].equals("-l")) {
        filter.setFilter("loose");
        continue;
      } else if (argv[i].equals("-n")) {
        filter.setRuleLength(Integer.parseInt(argv[i + 1]));
        i++;
        continue;
      }

      filter.loadTestSentences(argv[i]);
    }

    Scanner scanner = new Scanner(System.in, "UTF-8");
    int rulesIn = 0;
    int rulesOut = 0;
    if (filter.verbose) {
      System.err.println(String.format("Filtering rules with the %s filter...", filter.getFilterName()));
//      System.err.println("Using at max " + filter.RULE_LENGTH + " n-grams...");
    }
    while (scanner.hasNextLine()) {
      if (filter.verbose) {
        if ((rulesIn + 1) % 2000 == 0) {
          System.err.print(".");
          System.err.flush();
        }
        if ((rulesIn + 1) % 100000 == 0) {
          System.err.println(" [" + (rulesIn + 1) + "]");
          System.err.flush();
        }
      }
      rulesIn++;
      String rule = scanner.nextLine();

      String[] parts = P_DELIM.split(rule);
      if (parts.length >= 4) {
        String source = parts[1].trim();
        if (filter.inTestSet(source)) {
          System.out.println(rule);
          if (filter.parallel)
            System.out.flush();
          rulesOut++;
        } else if (filter.parallel) {
          System.out.println("");
          System.out.flush();
        }
      }
    }
    if (filter.verbose) {
      System.err.println("[INFO] Total rules read: " + rulesIn);
      System.err.println("[INFO] Rules kept: " + rulesOut);
      System.err.println("[INFO] Rules dropped: " + (rulesIn - rulesOut));
      System.err.println("[INFO] cached queries: " + filter.cached);
    }

    return;
  }
}
