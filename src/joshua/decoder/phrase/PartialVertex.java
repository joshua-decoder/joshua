package joshua.decoder.phrase;

/***
 * A PartialVertex represents a subset of a vertex. It is backed by a Vertex node and an index,
 * 
 *  
 */
public class PartialVertex {

  VertexNode back;
  int index;

  public PartialVertex() {}
  
  public PartialVertex(VertexNode back) {
    this.back = back;
    index = 0;
  }
  
  public PartialVertex(PartialVertex other) {
    this.back = other.back;
    this.index = other.index;
  }

  public boolean Empty() {
    return back.Empty();
  }
  
  public boolean Complete() {
    return back.Complete();
  }

  public ChartState State() {
    return back.State();
  }

  public boolean RightFull() { 
    return back.RightFull(); 
  }

  public float Bound() {
    return index > 0 ? back.get(index).Bound() : back.Bound();
  }

  public byte Niceness() { 
    return back.Niceness(); 
  }

  public Note End() {
    return back.End();
  }
}


