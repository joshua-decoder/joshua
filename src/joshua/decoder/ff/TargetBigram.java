package joshua.decoder.ff;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;	
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.util.io.LineReader;

/***
 * The RuleBigram feature is an indicator feature that counts target word bigrams that are created when
 * a rule is applied. It accepts three parameters:
 * 
 * -vocab /path/to/vocab
 * 
 *  The path to a vocabulary, where each line is of the format ID WORD COUNT.
 *  
 * -threshold N
 * 
 *  Mask to UNK all words whose COUNT is less than N.
 *  
 * -top-n N
 * 
 *  Only use the top N words.
 */

public class TargetBigram extends StatefulFF {
  
  private HashSet<String> vocab = null;
  private int maxTerms = 1000000;
  private int threshold = 0;

  public TargetBigram(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "TargetBigram", args, config);
    
    if (parsedArgs.containsKey("threshold"))
      threshold = Integer.parseInt(parsedArgs.get("threshold"));
    
    if (parsedArgs.containsKey("top-n"))
      maxTerms = Integer.parseInt(parsedArgs.get("top-n"));

    if (parsedArgs.containsKey("vocab")) {
      loadVocab(parsedArgs.get("vocab"));
    }
  }

  /**
   * Load vocabulary items passing the 'threshold' and 'top-n' filters.
   * 
   * @param filename
   */
  private void loadVocab(String filename) {
    this.vocab = new HashSet<String>(); 
    this.vocab.add("<s>");
    this.vocab.add("</s>");
    try {
      LineReader lineReader = new LineReader(filename);
      for (String line: lineReader) {
        if (lineReader.lineno() > maxTerms)
          break;
        
        String[] tokens = line.split("\\s+");
        String word = tokens[1];
        int count = Integer.parseInt(tokens[2]);
        
        if (count >= threshold)
          vocab.add(word);
      }

    } catch (IOException e) {
      System.err.println(String.format("* FATAL: couldn't load TargetBigram vocabulary '%s'", filename));
      System.exit(1);
    }
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int spanStart, int spanEnd,
      SourcePath sourcePath, Sentence sentence, Accumulator acc) {

    int[] enWords = rule.getEnglish();

    int left = -1;
    int right = -1;
    
    List<String> currentNgram = new LinkedList<String>();
    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);
        NgramDPState state = (NgramDPState) tailNodes.get(index).getDPState(stateIndex);
        int[] leftContext = state.getLeftLMStateWords();
        int[] rightContext = state.getRightLMStateWords();

        // Left context.
        for (int token : leftContext) {
          currentNgram.add(getWord(token));
          if (left == -1)
            left = token;
          right = token;
          if (currentNgram.size() == 2) {
            String ngram = join(currentNgram);
            acc.add(String.format("%s_%s", name, ngram), 1);
//            System.err.println(String.format("ADDING %s_%s", name, ngram));
            currentNgram.remove(0);
          }
        }
        // Replace right context.
        int tSize = currentNgram.size();
        for (int i = 0; i < rightContext.length; i++)
          currentNgram.set(tSize - rightContext.length + i, getWord(rightContext[i]));

      } else { // terminal words
        currentNgram.add(getWord(curID));
        if (left == -1)
          left = curID;
        right = curID;
        if (currentNgram.size() == 2) {
          String ngram = join(currentNgram);
          acc.add(String.format("%s_%s", name, ngram), 1);
//          System.err.println(String.format("ADDING %s_%s", name, ngram));
          currentNgram.remove(0);
        }
      }
    }

    NgramDPState state = new NgramDPState(new int[] { left }, new int[] { right });
//    System.err.println(String.format("RULE %s -> state %s", rule.getRuleString(), state));
    return state;
  }

  /**
   * Returns the word after comparing against the private vocabulary (if set).
   * 
   * @param curID
   * @return the word
   */
  private String getWord(int curID) {
    String word = Vocabulary.word(curID);

    if (vocab != null && ! vocab.contains(word)) {
      return "UNK"; 
    }
    
    return word;
  }

  /**
   * We don't compute a future cost.
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState state, Sentence sentence) {
    return 0.0f;
  }

  /**
   * There is nothing to be done here, since <s> and </s> are included in rules that are part
   * of the grammar. We simply return the DP state of the tail node.
   */
  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    
    return tailNode.getDPState(stateIndex);
  }

  /**
   * TargetBigram features are only computed across hyperedges, so there is nothing to be done here. 
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    return 0.0f;
  }

  /**
   * Join a list with the _ character. I am sure this is in a library somewhere.
   * 
   * @param list a list of strings
   * @return the joined String
   */
  private String join(List<String> list) {
    StringBuilder sb = new StringBuilder();
    for (String item : list) {
      sb.append(item.toString() + "_");
    }

    return sb.substring(0, sb.length() - 1);
  }
}
