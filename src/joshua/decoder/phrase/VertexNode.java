package joshua.decoder.phrase;  

import java.util.ArrayList;	
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VertexNode {

  private List<HypoState> hypos;
  private List<VertexNode> extend;
  private ChartState state;
  private boolean rightFull;
  private byte niceness;
  private float bound; // c++: type is score which is typedef floats

  public String toString() {
    return String.format("VN: (%d hypos) x (%d nodes)", hypos.size(), extend.size());
  }
  
  public VertexNode() {
    hypos = new ArrayList<HypoState>();
    extend = new ArrayList<VertexNode>();
  }

  public void InitRoot() {
    hypos.clear();
  }
  
  /**
   * Adds a hypothesis to the list, for later processing. This is used to build up the list
   * of hypotheses as we expand old ones, prior to cube pruning.
   * 
   * @param hypo
   */
  public void AppendHypothesis(HypoState hypo) {
    hypos.add(hypo);
    System.err.println(String.format("VertexNode::AppendHypothesis(%s)", hypo));
  }

  /**
   * Sorts the collected hypotheses to get ready for cube pruning.
   */
  public void FinishRoot() {
    // TODO: check correctness
    Collections.sort(hypos, new Comparator<HypoState>() {
      public int compare(HypoState a, HypoState b) {
        return a.score > b.score ? -1 : 0;
      }
    });

    extend.clear();
  }

  public boolean Complete() {
    return hypos.size() == 1 && extend.isEmpty();
  }

  public List<HypoState> Hypos() {
    return hypos;
  }

  public int Size() {
    return extend.size();
  }

  public float Bound() { // c++: typedef Score
    return bound;
  }

  public boolean Empty() {
    return hypos.isEmpty() && extend.isEmpty();
  }

  public ChartState State() {
    return state;
  }

  public boolean RightFull() {
    return rightFull;
  }
  
  public VertexNode get(int index) {
    return extend.get(index);
  }

  public byte Niceness() {
    return niceness;
  }

  public Note End() {
    // TODO Auto-generated method stub
    return null;
  }
}
