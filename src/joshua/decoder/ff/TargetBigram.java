package joshua.decoder.ff;

import java.util.LinkedList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class TargetBigram extends StatefulFF {

  public TargetBigram(FeatureVector weights, String name) {
    super(weights, name);

    // TODO Auto-generated constructor stub
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int spanStart, int spanEnd,
      SourcePath sourcePath, int sentID, Accumulator acc) {
    
    int[] enWords = rule.getEnglish();
    
    List<Integer> currentNgram = new LinkedList<Integer>();
    FeatureVector features = new FeatureVector();

    for (int c = 0; c < enWords.length; c++) {
      int curID = enWords[c];

      if (Vocabulary.nt(curID)) {
        int index = -(curID + 1);
        NgramDPState state = (NgramDPState) tailNodes.get(index).getDPState(stateIndex);
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
          acc.add(ngram, 1);
          currentNgram.remove(0);
        }
      }
    }

    // TODO 07/2013: use a real state here!
    return null;
  }

  @Override
  public float estimateFutureCost(Rule rule, DPState state, int sentID) {
    return 0.0f;
  }

  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
  
    NgramDPState state = (NgramDPState) tailNode.getDPState(stateIndex);
    int leftWord = state.getLeftLMStateWords()[0];
    int[] rightContext = state.getRightLMStateWords();
    int rightWord = rightContext[rightContext.length - 1];
    
    FeatureVector features = new FeatureVector();
    features.put("<s> " + leftWord, 1.0f);
    features.put(rightWord + " </s>", 1.0f);
    
    int[] left = new int[1];   left[0] = LanguageModelFF.START_SYM_ID; 
    int[] right = new int[1]; right[0] = LanguageModelFF.STOP_SYM_ID; 
    return new NgramDPState(left, right);
  }

  @Override
  public float estimateCost(Rule rule, int sentID) {
    return 0.0f;
  }

  private String join(List<Integer> list) {
    StringBuilder sb = new StringBuilder();
    for (Integer item : list) {
      sb.append(item.toString() + " ");
    }

    return sb.toString().trim();
  }
}
