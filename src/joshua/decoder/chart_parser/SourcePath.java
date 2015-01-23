package joshua.decoder.chart_parser;

import joshua.decoder.segment_file.Token;
import joshua.lattice.Arc;

/**
 * This class represents information about a path taken through the source lattice.
 * 
 * @note This implementation only tracks the source path cost which is assumed to be a scalar value.
 *       If you need multiple values, or want to recover more detailed path statistics, you'll need
 *       to update this code.
 */
public class SourcePath {

  private final float pathCost;

  public SourcePath() {
    pathCost = 0.0f;
  }

  private SourcePath(float cost) {
    pathCost = cost;
  }

  public float getPathCost() {
    return pathCost;
  }

  public SourcePath extend(Arc<Token> srcEdge) {
    float tcost = (float) srcEdge.getCost();
    if (tcost == 0.0)
      return this;
    else
      return new SourcePath(pathCost + (float) srcEdge.getCost());
  }

  public SourcePath extendNonTerminal() {
    return this;
  }

  public String toString() {
    return "SourcePath.cost=" + pathCost;
  }

}
