package joshua.decoder.ff;

import java.util.List;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.util.ListUtil;

public class LabelSubstitutionFF extends StatelessFF {
  private static final String LABEL_SUBSTITUTION_FEATURE_FUNCTION_NAME = "LabelSubstitution";
  private static final String MATCH_SUFFIX = "MATCH";
  private static final String NO_MATCH_SUFFIX = "NOMATCH";

  public LabelSubstitutionFF(FeatureVector weights) {
    super(weights, getLowerCasedFeatureName());
  }

  public static String getLowerCasedFeatureName() {
    return LABEL_SUBSTITUTION_FEATURE_FUNCTION_NAME.toLowerCase();
  }

  public static String getMatchFeatureSuffix(String ruleNonterminal, String substitutionNonterminal) {
    if (ruleNonterminal.equals(substitutionNonterminal)) {
      return MATCH_SUFFIX;
    } else {
      return NO_MATCH_SUFFIX;
    }
  }

  public static String getSubstitutionSuffix(String ruleNonterminal, String substitutionNonterminal) {
    return substitutionNonterminal + "_substitutes_" + ruleNonterminal;
  }

  private static final String computeLabelMatchingFeature(String ruleNonterminal,
      String substitutionNonterminal) {
    String result = getLowerCasedFeatureName() + "_";
    result += getMatchFeatureSuffix(ruleNonterminal, substitutionNonterminal);
    return result;
  }

  private static final String computeLabelSubstitutionFeature(String ruleNonterminal,
      String substitutionNonterminal) {
    String result = getLowerCasedFeatureName() + "_";
    result += getSubstitutionSuffix(ruleNonterminal, substitutionNonterminal);
    return result;
  }

  private static final String getRuleLabelsDescriptorString(Rule rule) {
    String result = "";
    String leftHandSide = RulePropertiesQuerying.getLHSAsString(rule);
    List<String> ruleSourceNonterminals = RulePropertiesQuerying
        .getRuleSourceNonterminalStrings(rule);
    boolean isInverting = rule.isInverting();
    result += "<LHS>" + leftHandSide + "</LHS>";
    result += "_<Nont>";
    result += ListUtil.stringListStringWithoutBracketsCommaSeparated(ruleSourceNonterminals);
    result += "</Nont>";
    if(isInverting)
    {  
      result += "_INV";
    }
    else
    {
      result += "_MONO";
    }
    
    return result;
  }

  private static final String getSubstitutionsDescriptorString(List<HGNode> tailNodes) {
    String result = "_<Subst>";
    List<String> substitutionNonterminals = RulePropertiesQuerying
        .getSourceNonterminalStrings(tailNodes);
    result += ListUtil.stringListStringWithoutBracketsCommaSeparated(substitutionNonterminals);
    result += "</Subst>";
    return result;
  }

  public static final String getGapLabelsForRuleSubstitutionSuffix(Rule rule, List<HGNode> tailNodes) {
    String result = getLowerCasedFeatureName() + "_";
    result += getRuleLabelsDescriptorString(rule);
    result += getSubstitutionsDescriptorString(tailNodes);
    return result;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
    if (rule != null && (tailNodes != null)) {

      List<String> ruleSourceNonterminals = RulePropertiesQuerying
          .getRuleSourceNonterminalStrings(rule);
      List<String> substitutionNonterminals = RulePropertiesQuerying
          .getSourceNonterminalStrings(tailNodes);
      // Assert.assertEquals(ruleSourceNonterminals.size(), substitutionNonterminals.size());
      for (int nonterinalIndex = 0; nonterinalIndex < ruleSourceNonterminals.size(); nonterinalIndex++) {
        String ruleNonterminal = ruleSourceNonterminals.get(nonterinalIndex);
        String substitutionNonterminal = substitutionNonterminals.get(nonterinalIndex);
        acc.add(computeLabelMatchingFeature(ruleNonterminal, substitutionNonterminal), 1);
        acc.add(computeLabelSubstitutionFeature(ruleNonterminal, substitutionNonterminal), 1);
      }
      acc.add(getGapLabelsForRuleSubstitutionSuffix(rule, tailNodes), 1);
    }
    return null;
  }

}
