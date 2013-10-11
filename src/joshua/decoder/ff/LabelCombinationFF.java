package joshua.decoder.ff;

import java.util.List;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class LabelCombinationFF extends StatelessFF {

  private static final String LABEL_COMINATION_FEATURE_FUNCTION_NAME = "LabelCombination";

  public LabelCombinationFF(FeatureVector weights) {
    super(weights, getLowerCasedFeatureName());
  }

  public static String getLowerCasedFeatureName() {
    return LABEL_COMINATION_FEATURE_FUNCTION_NAME.toLowerCase();
  }

  private static final String computeRuleLabelCombinationDescriptor(Rule rule) {
    String result = getLowerCasedFeatureName() + "_";
    result += RulePropertiesQuerying.getLHSAsString(rule);
    // System.out.println("Rule: " + rule);
    for (String foreignNonterminalString : RulePropertiesQuerying.getRuleSourceNonterminalStrings(rule)) {
      result += "_" + foreignNonterminalString;
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
