package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.phrase.Hypothesis;
import joshua.decoder.segment_file.Sentence;

/**
 * 
 * @author Zhifei Li <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public final class WordPenalty extends StatelessFF {

  private float OMEGA = -(float) Math.log10(Math.E); // -0.435

  public WordPenalty(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "WordPenalty", args, config);

    if (parsedArgs.containsKey("value"))
      OMEGA = Float.parseFloat(parsedArgs.get("value"));
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule != null) {
      // TODO: this is an inefficient way to do this. Find a better way to not apply this rule
      // to start and stop glue rules when phrase-based decoding.
      if (config.search_algorithm.equals("cky") 
          || (rule != Hypothesis.BEGIN_RULE && rule != Hypothesis.END_RULE))
        // acc.add(name, OMEGA * (rule.getEnglish().length - rule.getArity()));
        acc.add(denseFeatureIndex, OMEGA * (rule.getEnglish().length - rule.getArity()));
    }
      
    return null;
  }

  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    ArrayList<String> names = new ArrayList<String>();
    names.add(name);
    return names;
  }

  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    if (rule != null)
      return weights.getDense(denseFeatureIndex) * OMEGA * (rule.getEnglish().length - rule.getArity());
    return 0.0f;
  }
}
