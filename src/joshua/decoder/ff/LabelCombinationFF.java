package joshua.decoder.ff;

/***
 * @author Gideon Wenniger
 */

import java.util.List;	

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

public class LabelCombinationFF extends StatelessFF {

  public LabelCombinationFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LabelCombination", args, config);
  }

  public String getLowerCasedFeatureName() {
    return name.toLowerCase();
  }

  private final String computeRuleLabelCombinationDescriptor(Rule rule) {
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
      Sentence sentence, Accumulator acc) {
    if (rule != null)
      acc.add(computeRuleLabelCombinationDescriptor(rule), 1);

    return null;
  }

}
