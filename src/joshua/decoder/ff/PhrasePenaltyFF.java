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
public class PhrasePenaltyFF extends StatelessFF {

  private int owner = 0;
  
  public PhrasePenaltyFF(FeatureVector weights, String argString) {
    super(weights, "PhrasePenalty");
    
    String args[] = argString.split("\\s+");
    int i = 0;
    try {
      while (i < args.length) {
        if (args[i].startsWith("-")) {
          String key = args[i].substring(1);
          if (key.equals("owner")) {
            owner = Vocabulary.id(args[i+1]);
            System.err.println("PhrasePenalty: Setting owner to " + args[i+1]);
          } else {
            System.err.println(String.format("* FATAL: invalid PhrasePenaltyFF argument '%s'", key));
            System.exit(1);
          }
          i += 2;
        } else {
          i++;
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("* FATAL: Error processing PhrasePenaltyFF features");
      System.exit(1);
    }
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    if (owner > 0 && rule.getOwner() == owner)
      acc.add(name, 1);

    return null;
  }
}
