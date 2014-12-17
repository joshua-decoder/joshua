package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This feature returns the scored path through the source lattice, which is recorded in a
 * SourcePath object.
 * 
 * @author Chris Dyer <redpony@umd.edu>
 * @author Matt Post <post@cs.jhu.edu>
 */
public final class SourcePathFF extends StatelessFF {

  private static String SOURCE_PATH_FF_NAME = "sourcepath";
  
  /*
   * This is a single-value feature template, so we cache the weight here.
   */
  public SourcePathFF(FeatureVector weights) {
    super(weights, SOURCE_PATH_FF_NAME, ""); // this sets name
  }
  
  public String getFeatureName(){
    return SOURCE_PATH_FF_NAME;
  }
  
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    acc.add(name,  sourcePath.getPathCost());
    return null;
  }
}
