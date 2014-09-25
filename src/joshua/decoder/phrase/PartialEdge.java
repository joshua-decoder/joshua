package joshua.decoder.phrase;

public class PartialEdge extends Header {

  // Maintain a partial vertex for each vertex. For phrase-based decoding, there are two "vertices"
  private PartialVertex[] vertex;

  // This represents words between nonterminals, e.g., X -> x A y B z (one more than the arity)
  private ChartState[] between;
  
  public PartialEdge() {
    super();
  }
  
  public PartialEdge(int arity) {
    super(arity);
    
    vertex = new PartialVertex[arity];
    for (int i = 0; i < arity; i++)
      vertex[i] = new PartialVertex();
    between = new ChartState[arity + 1];
    for (int i = 0; i < arity + 1; i++)
      between[i] = new ChartState();
  }
  
  public PartialEdge(int num_vertices, int num_states) {
    super(num_vertices);

    vertex = new PartialVertex[num_vertices];
    between = new ChartState[num_states];
  }

  public PartialVertex[] NT() {
    return vertex;
  }

  public ChartState CompletedState() {
    return between[0];
  }
  
  public ChartState[] Between() {
    return between;
  }

  public Note End() {
    // TODO Auto-generated method stub
    return null;
  }
}
