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
    int leftWord = state.getLeftLMStateWords()[0];
    int[] rightContext = state.getRightLMStateWords();
    int rightWord = rightContext[rightContext.length - 1];
    
    FeatureVector features = new FeatureVector();
    features.put("<s> " + leftWord, 1.0f);
    features.put(rightWord + " </s>", 1.0f);
    
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
        int[] leftContext = state.getLeftLMStateWords();
        int[] rightContext = state.getRightLMStateWords();

        // Left context.
        for (int t : leftContext) {
          currentNgram.add(t);
          if (currentNgram.size() == 2) {
            String ngram = join(currentNgram);
            if (features.containsKey(ngram))
              features.put(ngram, 1);
            else
              features.put(ngram, features.get(ngram) + 1);
            currentNgram.remove(0);
          }
        }
        // Replace right context.
        int tSize = currentNgram.size();
        for (int i = 0; i < rightContext.length; i++)
          currentNgram.set(tSize - rightContext.length + i, rightContext[i]);
        
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
