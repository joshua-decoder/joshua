package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/*
 * Implements the RuleShape feature for source, target, and paired source+target sides.
 */
public class RuleShape extends StatelessFF {

  public RuleShape(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "RuleShape", args, config);
  }

  private int gettype(int id) {
    if (id < 0)
      return -1;
    return 1;
  }
  
  private String pattern(int[] ids) {
    String pattern = "";
    int curtype = gettype(ids[0]);
    int curcount = 1;
    for (int i = 1; i < ids.length; i++) {
      if (gettype(ids[i]) != curtype) {
        pattern += String.format("%s%s_", curtype < 0 ? "N" : "x", curcount > 1 ? "+" : "");
        curtype = gettype(ids[i]);
        curcount = 1;
      } else {
        curcount++;
      }
    }
    pattern += String.format("%s%s_", curtype < 0 ? "N" : "x", curcount > 1 ? "+" : "");
    return pattern;
  }
  
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i_, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    String sourceShape = pattern(rule.getFrench());
    String targetShape = pattern(rule.getEnglish());
    acc.add(String.format("%s_source_%s", name, sourceShape), 1);
    acc.add(String.format("%s_target_%s", name, targetShape), 1);
    acc.add(String.format("%s_both_%s__%s", name, sourceShape, targetShape), 1);

    return null;
  }
}
