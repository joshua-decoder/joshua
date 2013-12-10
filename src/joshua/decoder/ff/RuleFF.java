package joshua.decoder.ff;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

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
  
  public RuleFF(FeatureVector weights, String argString) {
    super(weights, "RuleFF");
    
    String args[] = argString.split("\\s+");
    int i = 0;
    try {
      while (i < args.length) {
        if (args[i].startsWith("-")) {
          String key = args[i].substring(1);
          if (key.equals("owner")) {
            owner = Vocabulary.id(args[i+1]);
            System.err.println("RuleFF: Setting owner to " + args[i+1]);
          } else if (key.equals("source")) {
            sides = Sides.SOURCE;
          } else if (key.equals("target")) {
            sides = Sides.TARGET;
          } else {
            System.err.println(String.format("* FATAL: invalid FragmentLMFF argument '%s'", key));
            System.exit(1);
          }
          i += 2;
        } else {
          i++;
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("* FATAL: Error processing RuleFF features");
      System.exit(1);
    }
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

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
