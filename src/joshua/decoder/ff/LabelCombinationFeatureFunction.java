package joshua.decoder.ff;

import java.util.List;
import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class LabelCombinationFeatureFunction extends StatelessFF {

  private static final String LABEL_COMINATION_FEATURE_FUNCTION_NAME = "labelcombinationfeature";

  public LabelCombinationFeatureFunction(FeatureVector weights) {
    super(weights, LABEL_COMINATION_FEATURE_FUNCTION_NAME);
  }

  public static String getLowerCasedFeatureName() {
    return LABEL_COMINATION_FEATURE_FUNCTION_NAME;
  }

  private static final String getLHSAsString(Rule rule) {
    return Vocabulary.word(rule.getLHS());
  }

  private static final String computeRuleLabelCombinationDescriptor(Rule rule) {
    String result = LABEL_COMINATION_FEATURE_FUNCTION_NAME + "_";
    result += getLHSAsString(rule);
    // System.out.println("Rule: " + rule);
    for (Integer nonTerminalIndex : rule.getForeignNonTerminals()) {
      result += "_" + Vocabulary.word(nonTerminalIndex);
    }
    return result;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
    if (rule != null)
      acc.add(computeRuleLabelCombinationDescriptor(rule), 1);

    return null;
  }

}
