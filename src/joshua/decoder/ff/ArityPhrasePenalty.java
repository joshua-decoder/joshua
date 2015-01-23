package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.decoder.chart_parser.SourcePath;
import joshua.corpus.Vocabulary;

/**
 * This feature function counts rules from a particular grammar (identified by the owner) having an
 * arity within a specific range. It expects three parameters upon initialization: the owner, the
 * minimum arity, and the maximum arity.
 * 
 * @author Matt Post <post@cs.jhu.edu
 * @author Zhifei Li <zhifei.work@gmail.com>
 */
public class ArityPhrasePenalty extends StatelessFF {

  // when the rule.arity is in the range, then this feature is activated
  private final int owner;
  private final int minArity;
  private final int maxArity;

  public ArityPhrasePenalty(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "ArityPenalty", args, config);

    this.owner = Vocabulary.id(parsedArgs.get("owner"));
    this.minArity = Integer.parseInt(parsedArgs.get("min-arity"));
    this.maxArity = Integer.parseInt(parsedArgs.get("max-arity"));

    if (!weights.containsKey(name))
      System.err.println("WARNING: no weight found for feature '" + name + "'");
  }

  /**
   * Returns 1 if the arity penalty feature applies to the current rule.
   */
  private int isEligible(final Rule rule) {
    if (this.owner == rule.getOwner() && rule.getArity() >= this.minArity
        && rule.getArity() <= this.maxArity)
      return 1;

    return 0;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    acc.add(name, isEligible(rule));
    
    return null;
  }
}
