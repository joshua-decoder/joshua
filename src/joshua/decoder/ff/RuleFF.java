package joshua.decoder.ff;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/**
 *  This feature just counts rules that are used. You can restrict it with a number of flags:
 * 
 *   -owner OWNER
 *    Only count rules owned by OWNER
 *   -target|-source
 *    Only count the target or source side (plus the LHS)
 *
 * TODO: add an option to separately provide a list of rule counts, restrict to counts above a threshold. 
 */
public class RuleFF extends StatelessFF {

  private enum Sides { SOURCE, TARGET, BOTH };
  
  private int owner = 0;
  private Sides sides = Sides.BOTH;
  
  public RuleFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "RuleFF", args, config);
    
    owner = Vocabulary.id(parsedArgs.get("owner"));
    if (parsedArgs.containsKey("source"))
      sides = Sides.SOURCE;
    else if (parsedArgs.containsKey("target"))
      sides = Sides.TARGET;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (owner > 0 && rule.getOwner() == owner) {
      String ruleString = getRuleString(rule);
      acc.add(ruleString, 1);
    }

    return null;
  }

  private String getRuleString(Rule rule) {
    String ruleString = "";
    switch(sides) {
    case BOTH:
      ruleString = String.format("%s  %s  %s", Vocabulary.word(rule.getLHS()), rule.getFrenchWords(),
          rule.getEnglishWords());
      break;

    case SOURCE:
      ruleString = String.format("%s  %s", Vocabulary.word(rule.getLHS()), rule.getFrenchWords());
      break;

    case TARGET:
      ruleString = String.format("%s  %s", Vocabulary.word(rule.getLHS()), rule.getEnglishWords());
      break;
    }
    return ruleString.replaceAll("[ =]", "~");
  }
}
