package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class TargetBigram extends StatefulFF {

  public TargetBigram(FeatureVector weights, String name, StateComputer stateComputer) {
    super(weights, name, stateComputer);
    // TODO Auto-generated constructor stub
  }

  @Override
  public FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID) {
    // TODO Auto-generated method stub
    return 0.0f;
  }

  @Override
  public float estimateFutureCost(Rule rule, DPState state, int sentID) {
    // TODO Auto-generated method stub
    return 0.0f;
  }

  @Override
  public FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public float estimateCost(Rule rule, int sentID) {
    // TODO Auto-generated method stub
    return 0;
  }

}
