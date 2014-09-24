package joshua.decoder.ff;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/*
 * This feature computes a bin for the rule and activates a feature for it. It requires access to
 * the index of the RarityPenalty field, from which the rule count can be computed.
 */
public class RuleCountBinFF extends StatelessFF {
  private int field = -1;

  public RuleCountBinFF(FeatureVector weights, String[] args) {
    super(weights, "RuleCountBin");

    if (args.length != 3 || !args[1].equals("-field")) {
      System.err.println("* FATAL: RuleCountBin: need -field N, N the RarityPenalty field");
      System.exit(-1);
    }

    field = Integer.parseInt(args[2]);
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    if (rule.getOwner() != Vocabulary.id("pt"))
      return null;
    
    float rarityPenalty = -rule.getFeatureVector().get(String.format("tm_pt_%d", field));
    int count = (int) (1.0 - Math.log(rarityPenalty));

    String feature = "RuleCountBin_inf";

    int[] bins = { 1, 2, 4, 8, 16, 32, 64, 128, 1000, 10000 };
    for (int k : bins) {
      if (count <= k) {
        feature = String.format("RuleCountBin_%d", k);
        break;
      }
    }

    System.err.println(String.format("RuleCountBin(%f) = %d ==> %s", rarityPenalty, count, feature));
    
    acc.add(feature, 1.0f);

    return null;
  }
}
