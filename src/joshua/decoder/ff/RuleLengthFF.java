package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/*
 * This feature computes three feature templates: a feature indicating the length of the rule's
 * source side, its target side, and a feature that pairs them.
 */
public class RuleLengthFF extends StatelessFF {

  public RuleLengthFF(FeatureVector weights) {
    super(weights, "RuleLength");
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
    int sourceLen = rule.getFrench().length;
    int targetLen = rule.getEnglish().length;
    acc.add(String.format("%s_sourceLength%d", name, sourceLen), 1);
    acc.add(String.format("%s_targetLength%d", name, targetLen), 1);
    acc.add(String.format("%s_pairLength%d-%d", name, sourceLen, targetLen), 1);

    return null;
  }
}
