package joshua.decoder.ff;

import java.util.LinkedList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class TargetBigram extends StatefulFF {

  public TargetBigram(FeatureVector weights, String name, StateComputer stateComputer) {
    super(weights, name, stateComputer);

    // TODO Auto-generated constructor stub
  }

  @Override
  public FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {
    return computeTransition(rule.getEnglish(), tailNodes);
  }

  @Override
  public float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID) {
    return computeTransition(rule.getEnglish(), tailNodes).innerProduct(this.weights);
  }

  @Override
  public float estimateFutureCost(Rule rule, DPState state, int sentID) {
    return 0.0f;
  }

  @Override
  public FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID) {
  
    NgramDPState state = (NgramDPState) tailNode.getDPState(this.getStateComputer());
    Integer leftWord = state.getLeftLMStateWords().get(0);
    List<Integer> rightContext = state.getRightLMStateWords();
    Integer rightWord = rightContext.get(rightContext.size() - 1);
    
    FeatureVector features = new FeatureVector();
    features.put("<s> " + leftWord.toString(), 1.0f);
    features.put(rightWord.toString() + " </s>", 1.0f);
    
    return features;
  }

  @Override
  public float estimateCost(Rule rule, int sentID) {
    return computeTransition(rule.getEnglish(), null).innerProduct(weights);
  }

  /**
   * This function computes all of the bigrams triggered by a rule application, and returns a
   * FeatureVector containing counts of those bigrams.
   * 
   * @param enWords
   * @param tailNodes
   * @param features
   */
  private FeatureVector computeTransition(int[] enWords, List<HGNode> tailNodes) {

    List<Integer> currentNgram = new LinkedList<Integer>();
    FeatureVector features = new FeatureVector();
    
    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);

        NgramDPState state = (NgramDPState) tailNodes.get(index)
            .getDPState(this.getStateComputer());
        List<Integer> leftContext = state.getLeftLMStateWords();
        List<Integer> rightContext = state.getRightLMStateWords();
        if (leftContext.size() != rightContext.size()) {
          throw new RuntimeException(
              "computeTransition: left and right contexts have unequal lengths");
        }

        // Left context.
        for (int i = 0; i < leftContext.size(); i++) {
          int t = leftContext.get(i);
          currentNgram.add(t);

          if (currentNgram.size() == 2) {
            // System.err.println(String.format("NGRAM(%s) = %.5f",
            // Vocabulary.getWords(currentNgram), prob));

            String ngram = join(currentNgram);
            if (features.containsKey(ngram))
              features.put(ngram, 1);
            else
              features.put(ngram, features.get(ngram) + 1);

            currentNgram.remove(0);
          }
        }

        // Right context.
        int tSize = currentNgram.size();
        for (int i = 0; i < rightContext.size(); i++) {
          // replace context
          currentNgram.set(tSize - rightContext.size() + i, rightContext.get(i));
        }

      } else { // terminal words
        currentNgram.add(curID);
        if (currentNgram.size() == 2) {
          String ngram = join(currentNgram);
          if (features.containsKey(ngram))
            features.put(ngram, 1);
          else
            features.put(ngram, features.get(ngram) + 1);

          currentNgram.remove(0);
        }
      }
    }
    return features;
  }

  private String join(List<Integer> list) {
    StringBuilder sb = new StringBuilder();
    for (Integer item : list) {
      sb.append(item.toString() + " ");
    }

    return sb.toString().trim();
  }
}
